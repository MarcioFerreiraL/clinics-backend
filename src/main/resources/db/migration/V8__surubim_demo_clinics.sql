-- Clínicas de demonstração ambientadas em Surubim-PE, com nomes, endereços e bairros levemente alterados
-- (de propósito, para não coincidir com estabelecimentos reais). Telefones e coordenadas são fictícios.
UPDATE clinics SET name = 'Clínica Santa Mariazinha', address = 'Rua João Alfredinho, 142', neighborhood = 'Centro',
       latitude = -7.833300, longitude = -35.752800
WHERE name = 'Clínica Santa Maria';

UPDATE clinics SET name = 'Laboratório São Zezinho', address = 'Av. São Sebastiãozinho, 310', neighborhood = 'São José',
       latitude = -7.841200, longitude = -35.760100
WHERE name = 'Laboratório São José';

UPDATE clinics SET name = 'Bioclínica Diagnóstikos', address = 'Rua Agamenon Magalhãzinho, 88', neighborhood = 'Centro',
       latitude = -7.831900, longitude = -35.754700
WHERE name = 'Bioclínica Diagnósticos';

UPDATE clinics SET name = 'Centro Médico Surubinzinho', address = 'Rua Barão do Rio Branquinho, 57', neighborhood = 'Bairro Novo',
       latitude = -7.826800, longitude = -35.746400
WHERE name = 'Centro Médico Surubim';

UPDATE clinics SET name = 'Policlínica do Agrestão', address = 'Av. Dr. Aprígio Guimarãezinho, 405', neighborhood = 'Cohab',
       latitude = -7.822100, longitude = -35.760500
WHERE name = 'Policlínica do Agreste';

UPDATE clinics SET name = 'Cardiocentro Surubim', address = 'Rua Sete de Setembrinho, 220', neighborhood = 'Santa Rita',
       latitude = -7.838600, longitude = -35.749400
WHERE name = 'Cardiocenter Surubim';
