-- Dados de demonstração (Surubim-PE). Valores fictícios.
INSERT INTO clinics (name, description, address, neighborhood, latitude, longitude, phone_number, whatsapp_number,
                     logo_url, rating, review_count, patient_count, is_verified)
VALUES
 ('Clínica Santa Maria', 'Diagnósticos por imagem e análises clínicas em Surubim. Exames com agilidade e entrega digital de laudos.',
  'Rua João Alfredo, 142', 'Centro', -7.833300, -35.752800, '(81) 3634-1122', '5581999999999',
  'https://images.unsplash.com/photo-1519494026892-80bbd2d6fd0d?w=150', 4.9, 128, 1200, TRUE),
 ('Laboratório São José', 'Tradição e confiabilidade em análises laboratoriais e exames de sangue em Surubim.',
  'Av. São Sebastião, 310', 'São José', -7.841200, -35.760100, '(81) 3634-2233', '5581988887777',
  'https://images.unsplash.com/photo-1579684385127-1ef15d508118?w=150', 4.8, 96, 950, TRUE),
 ('Bioclínica Diagnósticos', 'Centro integrado de ultrassonografia, cardiologia e exames preventivos com alta tecnologia.',
  'Rua Agamenon Magalhães, 88', 'Centro', -7.831900, -35.754700, '(81) 3634-3344', '5581997776666',
  'https://images.unsplash.com/photo-1629909613654-28e377c37b09?w=150', 4.7, 74, 780, TRUE),
 ('Centro Médico Surubim', 'Consultas especializadas e exames de imagem com plantão de laudos em 24 horas.',
  'Rua Barão do Rio Branco, 57', 'Centro', -7.834800, -35.751200, '(81) 3634-4455', '5581996665555',
  'https://images.unsplash.com/photo-1538108149393-fbbd81895907?w=150', 4.6, 61, 640, TRUE),
 ('Policlínica do Agreste', 'Clínica popular com consultas, exames laboratoriais e raio-x digital a preço justo.',
  'Av. Dr. Aprígio Guimarães, 405', 'Centro', -7.829500, -35.757300, '(81) 3634-5566', '5581995554444',
  'https://images.unsplash.com/photo-1586773860418-d37222d8fce3?w=150', 4.5, 52, 520, TRUE),
 ('Cardiocenter Surubim', 'Especializada em cardiologia: ECG, ecocardiograma, teste ergométrico e consultas.',
  'Rua Sete de Setembro, 220', 'Santa Rita', -7.838600, -35.749400, '(81) 3634-6677', '5581994443333',
  'https://images.unsplash.com/photo-1516549655169-df83a0774514?w=150', 4.8, 43, 410, TRUE);

-- Seg a Sex (dias 1 a 5) e, quando houver, sábado (dia 6)
INSERT INTO clinic_operating_hours (clinic_id, day_of_week, opens_at, closes_at)
SELECT c.id, d, v.weekday_open::time, v.weekday_close::time
FROM (VALUES
 ('Clínica Santa Maria', '06:30', '18:00'), ('Laboratório São José', '06:00', '17:00'),
 ('Bioclínica Diagnósticos', '07:00', '18:00'), ('Centro Médico Surubim', '07:00', '19:00'),
 ('Policlínica do Agreste', '06:30', '17:30'), ('Cardiocenter Surubim', '08:00', '17:00')
) AS v(clinic_name, weekday_open, weekday_close)
JOIN clinics c ON c.name = v.clinic_name
CROSS JOIN generate_series(1, 5) AS d;

INSERT INTO clinic_operating_hours (clinic_id, day_of_week, opens_at, closes_at)
SELECT c.id, 6, v.sat_open::time, v.sat_close::time
FROM (VALUES
 ('Clínica Santa Maria', '06:30', '12:00'), ('Laboratório São José', '06:00', '11:30'),
 ('Centro Médico Surubim', '07:00', '12:00')
) AS v(clinic_name, sat_open, sat_close)
JOIN clinics c ON c.name = v.clinic_name;

INSERT INTO exam_types (name, slug, category, description) VALUES
 ('Hemograma Completo', 'hemograma-completo', 'SANGUE', 'Análise de hemácias, leucócitos e plaquetas para avaliação geral da saúde.'),
 ('Glicemia de Jejum', 'glicemia-de-jejum', 'SANGUE', 'Dosagem de glicose no sangue em jejum.'),
 ('Lipidograma Completo', 'lipidograma-completo', 'SANGUE', 'Colesterol total, HDL, LDL e triglicerídeos.'),
 ('TSH e T4 Livre', 'tsh-e-t4-livre', 'SANGUE', 'Avaliação da função da tireoide.'),
 ('Ultrassom Abdominal Total', 'ultrassom-abdominal-total', 'ULTRASSOM', 'Ultrassom com Doppler para avaliação de fígado, vesícula, rins, pâncreas, baço e vasos abdominais.'),
 ('Ultrassom Obstétrico', 'ultrassom-obstetrico', 'ULTRASSOM', 'Acompanhamento do desenvolvimento fetal.'),
 ('Ultrassom de Tireoide', 'ultrassom-de-tireoide', 'ULTRASSOM', 'Avaliação de nódulos e volume da tireoide.'),
 ('Eletrocardiograma (ECG)', 'eletrocardiograma-ecg', 'CARDIO', 'Registro da atividade elétrica do coração.'),
 ('Ecocardiograma', 'ecocardiograma', 'CARDIO', 'Ultrassom do coração com Doppler colorido.'),
 ('Teste Ergométrico', 'teste-ergometrico', 'CARDIO', 'Avaliação do coração sob esforço físico.'),
 ('MAPA 24 horas', 'mapa-24-horas', 'CARDIO', 'Monitorização ambulatorial da pressão arterial.'),
 ('Raio-X de Tórax', 'raio-x-de-torax', 'RAIO_X', 'Imagem digital do tórax.'),
 ('Raio-X de Coluna Lombar', 'raio-x-de-coluna-lombar', 'RAIO_X', 'Radiografia digital da coluna lombar.'),
 ('Raio-X de Membros', 'raio-x-de-membros', 'RAIO_X', 'Radiografia de braços, pernas, mãos e pés.'),
 ('Consulta Clínico Geral', 'consulta-clinico-geral', 'CONSULTAS', 'Consulta com médico clínico geral.'),
 ('Consulta Cardiologia', 'consulta-cardiologia', 'CONSULTAS', 'Consulta com cardiologista.'),
 ('Consulta Ginecologia', 'consulta-ginecologia', 'CONSULTAS', 'Consulta com ginecologista.'),
 ('Consulta Pediatria', 'consulta-pediatria', 'CONSULTAS', 'Consulta com pediatra.');

INSERT INTO exams (clinic_id, exam_type_id, price, preparation, payment_methods)
SELECT c.id, t.id, v.price, v.preparation, v.payment_methods
FROM (VALUES
 ('Clínica Santa Maria', 'Ultrassom Abdominal Total', 90.00, 'Jejum 8h • Laudo em 24h', 'Pix à vista ou até 3x no cartão'),
 ('Clínica Santa Maria', 'Hemograma Completo', 20.00, 'Jejum 4h • Coleta até 11:00', 'À vista (Pix / Dinheiro)'),
 ('Clínica Santa Maria', 'Eletrocardiograma (ECG)', 45.00, 'Sem preparo • Laudo com cardiologista', 'À vista ou cartão'),
 ('Clínica Santa Maria', 'Raio-X de Tórax', 55.00, 'Sem preparo • Resultado no mesmo dia', 'À vista ou cartão'),
 ('Clínica Santa Maria', 'Lipidograma Completo', 28.00, 'Jejum 12h', 'À vista (Pix / Dinheiro)'),
 ('Laboratório São José', 'Hemograma Completo', 18.00, 'Jejum de 4 a 8h • Resultado em 24h', 'À vista (Pix / Dinheiro)'),
 ('Laboratório São José', 'Glicemia de Jejum', 8.00, 'Jejum 8h', 'À vista (Pix / Dinheiro)'),
 ('Laboratório São José', 'Lipidograma Completo', 30.00, 'Jejum 12h', 'À vista ou cartão'),
 ('Laboratório São José', 'TSH e T4 Livre', 42.00, 'Jejum 4h', 'À vista ou cartão'),
 ('Laboratório São José', 'Ultrassom Abdominal Total', 110.00, 'Jejum 8h', 'Até 2x sem juros ou Pix'),
 ('Bioclínica Diagnósticos', 'Ultrassom Abdominal Total', 120.00, 'Jejum 8h • Laudo em 24h', 'Cartão em até 3x ou Pix'),
 ('Bioclínica Diagnósticos', 'Ultrassom Obstétrico', 100.00, 'Bexiga cheia, conforme idade gestacional', 'À vista ou cartão'),
 ('Bioclínica Diagnósticos', 'Ultrassom de Tireoide', 95.00, 'Sem preparo', 'À vista ou cartão'),
 ('Bioclínica Diagnósticos', 'Eletrocardiograma (ECG)', 50.00, 'Sem preparo', 'À vista ou cartão'),
 ('Bioclínica Diagnósticos', 'Raio-X de Coluna Lombar', 65.00, 'Sem preparo', 'À vista ou cartão'),
 ('Centro Médico Surubim', 'Ultrassom Abdominal Total', 130.00, 'Jejum 8h • Plantão com laudo em 24h', 'Cartão em até 3x ou Pix'),
 ('Centro Médico Surubim', 'Consulta Clínico Geral', 120.00, 'Agendamento prévio', 'À vista ou cartão'),
 ('Centro Médico Surubim', 'Consulta Cardiologia', 180.00, 'Agendamento prévio', 'À vista ou cartão'),
 ('Centro Médico Surubim', 'Raio-X de Tórax', 60.00, 'Sem preparo', 'À vista ou cartão'),
 ('Centro Médico Surubim', 'Consulta Ginecologia', 150.00, 'Agendamento prévio', 'À vista ou cartão'),
 ('Policlínica do Agreste', 'Ultrassom Abdominal Total', 140.00, 'Jejum 8h', 'Cartão em até 3x ou Pix'),
 ('Policlínica do Agreste', 'Consulta Clínico Geral', 80.00, 'Agendamento prévio', 'À vista (Pix / Dinheiro)'),
 ('Policlínica do Agreste', 'Raio-X de Tórax', 45.00, 'Sem preparo', 'À vista ou cartão'),
 ('Policlínica do Agreste', 'Raio-X de Membros', 40.00, 'Sem preparo', 'À vista ou cartão'),
 ('Policlínica do Agreste', 'Hemograma Completo', 22.00, 'Jejum 4h', 'À vista (Pix / Dinheiro)'),
 ('Policlínica do Agreste', 'Consulta Pediatria', 100.00, 'Agendamento prévio', 'À vista ou cartão'),
 ('Cardiocenter Surubim', 'Eletrocardiograma (ECG)', 40.00, 'Sem preparo', 'À vista ou cartão'),
 ('Cardiocenter Surubim', 'Ecocardiograma', 160.00, 'Sem preparo', 'Até 3x sem juros ou Pix'),
 ('Cardiocenter Surubim', 'Teste Ergométrico', 150.00, 'Roupa leve • Evitar cafeína 4h antes', 'À vista ou cartão'),
 ('Cardiocenter Surubim', 'Consulta Cardiologia', 170.00, 'Agendamento prévio', 'À vista ou cartão'),
 ('Cardiocenter Surubim', 'MAPA 24 horas', 130.00, 'Agendar instalação do aparelho', 'À vista ou cartão')
) AS v(clinic_name, type_name, price, preparation, payment_methods)
JOIN clinics c ON c.name = v.clinic_name
JOIN exam_types t ON t.name = v.type_name;

INSERT INTO price_history (exam_id, price, recorded_at)
SELECT id, price, now() - interval '30 days' FROM exams;

-- Contatos anônimos nos últimos 6 dias, para alimentar o ranking "Em alta"
INSERT INTO contact_events (exam_id, user_id, price_at_contact, estimated_savings, created_at)
SELECT e.id, NULL, e.price, 0, now() - (random() * interval '6 days')
FROM exams e
CROSS JOIN LATERAL generate_series(1, 1 + (abs(hashtext(e.id::text)) % 8)) AS g;
