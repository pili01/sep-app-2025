DELETE FROM public.vehicles;
DELETE FROM public.insurances;

ALTER SEQUENCE public.insurances_id_seq RESTART WITH 1;
ALTER SEQUENCE public.vehicles_id_seq RESTART WITH 1;


INSERT INTO public.insurances (
    id,
    company,
    name,
    price_per_day,
    deleted
)
VALUES
    (1, 'Allianz',  'Basic Insurance',    10, false),
    (2, 'Generali', 'Premium Insurance',  15, false),
    (3, 'Dunav',    'Standard Insurance', 12, false),
    (4, 'Wiener',   'Full Coverage',      20, false),
    (5, 'Triglav',  'Economy Insurance',   8, false);

INSERT INTO public.vehicles (
    id,
    chassis_number,
    picture_url,
    price_per_day,
    registration,
    type,
    deleted
)
VALUES
    (2, 'CHASSIS001', '1', 50, 'BG-123-AB', 'Sedan',     false),
    (3, 'CHASSIS002', '2', 75, 'BG-456-CD', 'SUV',       false),
    (4, 'CHASSIS003', '3', 60, 'BG-789-EF', 'Hatchback', false),
    (5, 'CHASSIS004', '4', 90, 'BG-012-GH', 'Coupe',     false),
    (6, 'CHASSIS005', '5', 55, 'BG-345-IJ', 'Sedan',     false);
