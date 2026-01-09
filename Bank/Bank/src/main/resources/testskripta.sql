DELETE FROM public.card;
DELETE FROM public.account;

ALTER SEQUENCE public.account_id_seq RESTART WITH 1;
ALTER SEQUENCE public.card_id_seq RESTART WITH 1;

INSERT INTO public.account (merchant_id, account_number, balance, currency, deleted)
VALUES (
    'TEST_MERCHANT_1',
    '170-1111111111-111',
    0.0,
    'EUR',
    false
);

INSERT INTO public.account (merchant_id, account_number, balance, currency, deleted)
VALUES (
    'CUSTOMER_USER_1',
    '180-1111111111-111',
    1000.0,
    'EUR',
    false
);

INSERT INTO public.card (
    card_number,
    cardholder_name,
    expiration_date,
    cvv,
    deleted,
    account_id
)
VALUES (
    '4111111111111111',
    'Ognjen Papovic',
    '12/26',
    '123',
    false,
    (SELECT id FROM public.account WHERE account_number = '180-1111111111-111')
);
