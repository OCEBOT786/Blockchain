-- auth user -> profiles trigger
create or replace function public.handle_new_user()
returns trigger
language plpgsql
security definer set search_path = ''
as $$
begin
    insert into public.profiles (
        id,
        role,
        full_name,
        company_name,
        wallet_address,
        phone
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
for each row
execute procedure public.handle_new_user();


-- API permissions

grant usage on schema public to authenticated;

grant select, update
on table public.profiles
to authenticated;

grant select, insert, update
on table public.orders
to authenticated;


-- order RLS polucies

drop policy if exists "Buyers can create own orders"
on public.orders;

create policy "Buyers can create own orders"
on public.orders
for insert
to authenticated
with check (
    auth.uid() = buyer_id
);


drop policy if exists "Participants can update orders"
on public.orders;

create policy "Participants can update orders"
on public.orders
for update
to authenticated
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

create or replace function public.set_updated_at()
returns trigger
language plpgsql
as $$
begin
    new.updated_at = now();
    return new;
end;
$$;

drop trigger if exists set_profiles_updated_at
on public.profiles;

create trigger set_profiles_updated_at
before update on public.profiles
for each row
execute function public.set_updated_at();


drop trigger if exists set_orders_updated_at
on public.orders;

create trigger set_orders_updated_at
before update on public.orders
for each row
execute function public.set_updated_at();

-- realtime
do $$
begin
    if exists (
        select 1
        from pg_publication
        where pubname = 'supabase_realtime'
    )
    and not exists (
        select 1
        from pg_publication_tables
        where pubname = 'supabase_realtime'
          and schemaname = 'public'
          and tablename = 'orders'
    )
    then
        execute
            'alter publication supabase_realtime add table public.orders';
    end if;
end
$$;