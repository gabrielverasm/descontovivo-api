CREATE TABLE store (
    id UUID PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    slug VARCHAR(100) NOT NULL UNIQUE,
    url VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT now()
);

INSERT INTO store (id, name, slug, url, created_at) VALUES
    (gen_random_uuid(), 'Amazon', 'amazon', 'https://www.amazon.com.br', now()),
    (gen_random_uuid(), 'Mercado Livre', 'mercado-livre', 'https://www.mercadolivre.com.br', now()),
    (gen_random_uuid(), 'Magalu', 'magalu', 'https://www.magazineluiza.com.br', now());
