USE pagoflex_pagos;

ALTER TABLE pagos
ADD COLUMN id_compromiso_activo BIGINT
GENERATED ALWAYS AS (
    CASE
        WHEN activo = 1 THEN id_compromiso
        ELSE NULL
    END
) PERSISTENT,
ADD UNIQUE KEY uq_pago_compromiso_activo (
    id_compromiso_activo
);
