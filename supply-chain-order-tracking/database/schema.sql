create table if not exists public.profiles (
    id uuid primary key references auth.users(id) on delete cascade,
    role text not null check (role in ('buyer', 'wholesaler', 'logistics_provider')),
    full_name text not null,
    company_name text,
    wallet_address text constraint profiles_wallet_address_key unique,
    phone text,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create table if not exists public.orders (
    id uuid primary key default gen_random_uuid(),
    blockchain_order_id bigint check (blockchain_order_id > 0),
    buyer_id uuid not null references public.profiles(id) on delete no action,
    wholesaler_id uuid references public.profiles(id) on delete set null,
    logistics_provider_id uuid references public.profiles(id) on delete set null,
    product_name text not null,
    product_description text,
    quantity integer not null check (quantity > 0),
    delivery_address text,
    notes text,
    blockchain_tx_hash text,
    contract_address text,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    constraint orders_blockchain_link_complete check (
        (blockchain_order_id is null and blockchain_tx_hash is null and contract_address is null)
        or
        (blockchain_order_id is not null and blockchain_tx_hash is not null and contract_address is not null)
    )
);

-- Order IDs restart with each deployment; checksum casing is ignored.
create unique index if not exists orders_contract_blockchain_id_key
    on public.orders (lower(contract_address), blockchain_order_id)
    where blockchain_order_id is not null;

create or replace function public.set_updated_at()
returns trigger language plpgsql set search_path = ''
as $$
begin
    new.updated_at = now();
    return new;
end;
$$;

drop trigger if exists set_profiles_updated_at on public.profiles;
create trigger set_profiles_updated_at
before update on public.profiles
for each row execute function public.set_updated_at();

drop trigger if exists set_orders_updated_at on public.orders;
create trigger set_orders_updated_at
before update on public.orders
for each row execute function public.set_updated_at();

-- Read the assigned users' roles without exposing other profiles through RLS.
create or replace function public.validate_order_roles()
returns trigger language plpgsql security definer set search_path = ''
as $$
begin
    if not exists (
        select 1 from public.profiles
        where id = new.buyer_id and role = 'buyer'
    ) then
        raise exception 'buyer_id must reference a buyer profile';
    end if;
    if new.wholesaler_id is not null and not exists (
        select 1 from public.profiles
        where id = new.wholesaler_id and role = 'wholesaler'
    ) then
        raise exception 'wholesaler_id must reference a wholesaler profile';
    end if;
    if new.logistics_provider_id is not null and not exists (
        select 1 from public.profiles
        where id = new.logistics_provider_id and role = 'logistics_provider'
    ) then
        raise exception 'logistics_provider_id must reference a logistics provider profile';
    end if;
    return new;
end;
$$;

drop trigger if exists validate_order_roles on public.orders;
create trigger validate_order_roles
before insert or update on public.orders
for each row execute function public.validate_order_roles();

-- Only the buyer may assign participants or add the first blockchain link.
-- Linked fields are immutable to authenticated users after that first link.
create or replace function public.protect_order_update()
returns trigger language plpgsql set search_path = ''
as $$
begin
    if auth.uid() is not null then
        if new.buyer_id is distinct from old.buyer_id then
            raise exception 'buyer_id cannot be changed';
        end if;
        if (
            new.wholesaler_id is distinct from old.wholesaler_id
            or new.logistics_provider_id is distinct from old.logistics_provider_id
        ) and auth.uid() is distinct from old.buyer_id then
            raise exception 'only the buyer can change participant assignments';
        end if;
        if (
            new.blockchain_order_id is distinct from old.blockchain_order_id
            or new.blockchain_tx_hash is distinct from old.blockchain_tx_hash
            or new.contract_address is distinct from old.contract_address
        ) then
            if auth.uid() is distinct from old.buyer_id then
                raise exception 'only the buyer can link an order to the blockchain';
            end if;
            if old.blockchain_order_id is not null then
                raise exception 'an existing blockchain link cannot be changed';
            end if;
        end if;
    end if;
    return new;
end;
$$;

drop trigger if exists protect_order_update on public.orders;
create trigger protect_order_update
before update on public.orders
for each row execute function public.protect_order_update();

-- Supabase Auth creates a profile from signup metadata.
create or replace function public.handle_new_user()
returns trigger language plpgsql security definer set search_path = ''
as $$
begin
    insert into public.profiles (
        id, role, full_name, company_name, wallet_address, phone
    )
    values (
        new.id,
        new.raw_user_meta_data ->> 'role',
        new.raw_user_meta_data ->> 'full_name',
        new.raw_user_meta_data ->> 'company_name',
        new.raw_user_meta_data ->> 'wallet_address',
        new.raw_user_meta_data ->> 'phone'
    );
    return new;
end;
$$;

drop trigger if exists on_auth_user_created on auth.users;
create trigger on_auth_user_created
after insert on auth.users
for each row execute function public.handle_new_user();

alter table public.profiles enable row level security;
alter table public.orders enable row level security;

drop policy if exists "Users can read own profile" on public.profiles;
create policy "Users can read own profile"
on public.profiles for select to authenticated
using (auth.uid() = id);

drop policy if exists "Users can update own profile" on public.profiles;
create policy "Users can update own profile"
on public.profiles for update to authenticated
using (auth.uid() = id)
with check (auth.uid() = id);

drop policy if exists "Participants can read orders" on public.orders;
create policy "Participants can read orders"
on public.orders for select to authenticated
using (
    auth.uid() = buyer_id
    or auth.uid() = wholesaler_id
    or auth.uid() = logistics_provider_id
);

drop policy if exists "Buyers can create own orders" on public.orders;
create policy "Buyers can create own orders"
on public.orders for insert to authenticated
with check (
    auth.uid() = buyer_id
    and exists (
        select 1 from public.profiles
        where id = auth.uid() and role = 'buyer'
    )
    and blockchain_order_id is null
    and blockchain_tx_hash is null
    and contract_address is null
);

drop policy if exists "Participants can update orders" on public.orders;
create policy "Participants can update orders"
on public.orders for update to authenticated
using (
    auth.uid() = buyer_id
    or auth.uid() = wholesaler_id
    or auth.uid() = logistics_provider_id
)
with check (
    auth.uid() = buyer_id
    or auth.uid() = wholesaler_id
    or auth.uid() = logistics_provider_id
);

-- Remove broad default grants. Role and buyer_id remain read-only to
-- authenticated REST clients; the trigger further protects assignments.
revoke all on table public.profiles from public, anon, authenticated;
revoke all on table public.orders from public, anon, authenticated;
grant usage on schema public to authenticated;
grant select on table public.profiles to authenticated;
grant update (full_name, company_name, wallet_address, phone)
on table public.profiles to authenticated;
grant select on table public.orders to authenticated;
grant insert (
    buyer_id, wholesaler_id, logistics_provider_id,
    product_name, product_description, quantity, delivery_address, notes
) on table public.orders to authenticated;
grant update (
    wholesaler_id, logistics_provider_id, product_name, product_description,
    quantity, delivery_address, notes, blockchain_order_id,
    blockchain_tx_hash, contract_address
) on table public.orders to authenticated;

-- Realtime delivers an order event only when the subscriber can select it.
do $$
begin
    if not exists (
        select 1 from pg_publication where pubname = 'supabase_realtime'
    ) then
        execute 'create publication supabase_realtime';
    end if;
    if not exists (
        select 1 from pg_publication_tables
        where pubname = 'supabase_realtime'
          and schemaname = 'public'
          and tablename = 'orders'
    ) then
        execute 'alter publication supabase_realtime add table public.orders';
    end if;
end;
$$;
