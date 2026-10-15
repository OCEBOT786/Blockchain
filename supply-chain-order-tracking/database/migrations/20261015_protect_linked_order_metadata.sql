create or replace function public.protect_order_update()
returns trigger language plpgsql set search_path = ''
as $$
begin
    if (old.blockchain_order_id is not null or new.blockchain_order_id is not null) and (
        new.id is distinct from old.id
        or new.buyer_id is distinct from old.buyer_id
        or new.product_name is distinct from old.product_name
        or new.product_description is distinct from old.product_description
        or new.quantity is distinct from old.quantity
        or new.delivery_address is distinct from old.delivery_address
    ) then
        raise exception 'linked order metadata cannot be changed';
    end if;
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
