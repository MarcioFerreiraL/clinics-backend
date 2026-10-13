-- Pacientes e avaliações de demonstração. Mantêm clinics.rating e review_count coerentes com a tabela reviews (RN27).
-- O hash abaixo não é um BCrypt válido: essas contas existem só para assinar avaliações e não conseguem fazer login.
INSERT INTO users (phone, password_hash, full_name) VALUES
 ('5581900000001', '!demo-sem-login', 'Maria Vasconcelos'),
 ('5581900000002', '!demo-sem-login', 'João Pereira'),
 ('5581900000003', '!demo-sem-login', 'Ana Beatriz Lima'),
 ('5581900000004', '!demo-sem-login', 'Carlos Eduardo Silva'),
 ('5581900000005', '!demo-sem-login', 'Fernanda Costa');

INSERT INTO reviews (clinic_id, user_id, rating, comment, created_at, updated_at)
SELECT c.id, u.id, v.rating, v.comment, now() - (v.days || ' days')::interval, now() - (v.days || ' days')::interval
FROM (VALUES
 ('Clínica Santa Maria', '5581900000001', 5, 'Atendimento rápido e laudo no mesmo dia.', 20),
 ('Clínica Santa Maria', '5581900000002', 5, 'Preço justo e equipe atenciosa.', 18),
 ('Clínica Santa Maria', '5581900000003', 5, NULL, 15),
 ('Clínica Santa Maria', '5581900000004', 5, 'Fiz o ultrassom sem espera.', 9),
 ('Clínica Santa Maria', '5581900000005', 4, 'Boa clínica, estacionamento é difícil.', 3),
 ('Laboratório São José', '5581900000001', 5, 'Coleta tranquila e resultado em 24h.', 25),
 ('Laboratório São José', '5581900000002', 5, NULL, 17),
 ('Laboratório São José', '5581900000003', 4, 'Resultado atrasou um pouco.', 12),
 ('Laboratório São José', '5581900000004', 5, 'Tradicional e confiável.', 6),
 ('Bioclínica Diagnósticos', '5581900000001', 5, 'Ótimo ultrassom obstétrico.', 22),
 ('Bioclínica Diagnósticos', '5581900000002', 4, NULL, 14),
 ('Bioclínica Diagnósticos', '5581900000003', 5, 'Equipamentos modernos.', 8),
 ('Centro Médico Surubim', '5581900000002', 5, 'Laudo em 24 horas, como prometido.', 19),
 ('Centro Médico Surubim', '5581900000004', 4, NULL, 11),
 ('Centro Médico Surubim', '5581900000005', 4, 'Consulta pontual.', 5),
 ('Policlínica do Agreste', '5581900000001', 4, 'Preço popular.', 21),
 ('Policlínica do Agreste', '5581900000003', 5, 'Atendimento educado.', 13),
 ('Policlínica do Agreste', '5581900000005', 4, 'Fila grande no começo da manhã.', 4),
 ('Cardiocenter Surubim', '5581900000002', 5, 'Cardiologista excelente.', 16),
 ('Cardiocenter Surubim', '5581900000003', 5, 'ECG feito na hora.', 10),
 ('Cardiocenter Surubim', '5581900000004', 4, NULL, 2)
) AS v(clinic_name, phone, rating, comment, days)
JOIN clinics c ON c.name = v.clinic_name
JOIN users u ON u.phone = v.phone;

UPDATE clinics c
SET rating = s.avg_rating, review_count = s.total
FROM (SELECT clinic_id, ROUND(AVG(rating), 1) AS avg_rating, COUNT(*) AS total FROM reviews GROUP BY clinic_id) s
WHERE c.id = s.clinic_id;
