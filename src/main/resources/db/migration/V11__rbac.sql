-- RBAC: cargos, permissões, lotação dos servidores internos e auditoria.
-- Só adiciona tabelas e linhas; clinics.owner_user_id continua na tabela, sem uso.

CREATE TABLE positions (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name             VARCHAR(60) NOT NULL UNIQUE,
    visibility_scope VARCHAR(20) NOT NULL,
    CONSTRAINT ck_positions_scope CHECK (visibility_scope IN ('all', 'own_clinic'))
);

CREATE TABLE permissions (
    id       UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    resource VARCHAR(40)  NOT NULL,
    action   VARCHAR(40)  NOT NULL,
    label    VARCHAR(120) NOT NULL,
    CONSTRAINT uq_permissions UNIQUE (resource, action)
);

CREATE TABLE position_permissions (
    position_id   UUID NOT NULL REFERENCES positions (id) ON DELETE CASCADE,
    permission_id UUID NOT NULL REFERENCES permissions (id) ON DELETE CASCADE,
    PRIMARY KEY (position_id, permission_id)
);

-- clinic_id é nulo só para cargos com escopo "all" (Administrador).
CREATE TABLE internal_users (
    user_id     UUID PRIMARY KEY REFERENCES users (id) ON DELETE CASCADE,
    position_id UUID NOT NULL REFERENCES positions (id),
    clinic_id   UUID REFERENCES clinics (id) ON DELETE CASCADE
);

CREATE INDEX idx_internal_users_clinic ON internal_users (clinic_id);

CREATE TABLE permission_audit_logs (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    actor_user_id UUID        NOT NULL,
    action        VARCHAR(20) NOT NULL,
    position_id   UUID,
    permission_id UUID,
    target_user_id UUID,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ck_audit_action CHECK (action IN ('grant', 'revoke', 'position_change'))
);

INSERT INTO positions (name, visibility_scope) VALUES
    ('Administrador', 'all'),
    ('Gestor da clínica', 'own_clinic'),
    ('Atendente', 'own_clinic');

INSERT INTO permissions (resource, action, label) VALUES
    ('clinica', 'ver', 'Ver os dados da clínica'),
    ('clinica', 'editar', 'Editar os dados da clínica'),
    ('exame', 'ver', 'Ver as ofertas de exame'),
    ('exame', 'editar', 'Cadastrar e editar ofertas de exame'),
    ('exame', 'desativar', 'Desativar ofertas de exame'),
    ('indicadores', 'ver', 'Ver os indicadores da clínica'),
    ('conta', 'listar', 'Listar contas internas'),
    ('conta', 'gerenciar', 'Trocar o cargo de uma conta'),
    ('permissao', 'gerenciar', 'Editar a matriz de permissões');

-- Administrador: todas as permissões.
INSERT INTO position_permissions
SELECT p.id, m.id FROM positions p CROSS JOIN permissions m WHERE p.name = 'Administrador';

-- Gestor: clínica, exame e indicadores.
INSERT INTO position_permissions
SELECT p.id, m.id FROM positions p CROSS JOIN permissions m
WHERE p.name = 'Gestor da clínica' AND m.resource IN ('clinica', 'exame', 'indicadores');

-- Atendente: só leitura.
INSERT INTO position_permissions
SELECT p.id, m.id FROM positions p CROSS JOIN permissions m
WHERE p.name = 'Atendente' AND m.action = 'ver';

-- Quem já era gestor ou administrador mantém o acesso.
INSERT INTO internal_users (user_id, position_id, clinic_id)
SELECT c.owner_user_id, (SELECT id FROM positions WHERE name = 'Gestor da clínica'), c.id
FROM clinics c WHERE c.owner_user_id IS NOT NULL;

INSERT INTO internal_users (user_id, position_id, clinic_id)
SELECT u.id, (SELECT id FROM positions WHERE name = 'Administrador'), NULL
FROM users u WHERE u.role = 'ADMIN'
ON CONFLICT (user_id) DO NOTHING;
