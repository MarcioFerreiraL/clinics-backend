# 11 · Testes de carga e desempenho

Ferramenta: **k6** (scripts em `loadtest/`). Meta principal: **RNF03** — busca com p95 abaixo de 300 ms.

## Como rodar

```bash
# ambiente espelho da produção (2 CPUs, API 600 MB, banco 200 MB com os mesmos parâmetros)
docker build -t clinics-api:latest .
docker compose -f loadtest/compose.loadtest.yaml -p clinicsload up -d

loadtest/run.sh smoke     # 1 usuário, 30 s: todos os fluxos respondem
loadtest/run.sh load      # carga nominal: 30 visitantes + 10 pacientes por ~3 min
loadtest/run.sh stress    # taxa crescente de requisições até saturar
loadtest/run.sh spike     # salto súbito para 150 usuários e recuperação
DURATION=30m loadtest/run.sh soak   # resistência (vazamento de memória)
loadtest/run.sh latency https://clinics.marciodev.com   # só leitura, para produção
```

Os resultados (JSON) ficam em `loadtest/results/` (fora do Git). No GitHub: **Actions → Testes de carga → Run workflow**; também roda toda segunda de manhã.

## O que cada cenário simula

| Cenário | Perfil | Critério de aprovação |
|---|---|---|
| `smoke` | 1 usuário percorre busca, detalhe, cadastro, perfil e economia | 0% de erro, p95 < 500 ms |
| `load` | Visitantes navegam (busca 60%, detalhe, clínica, em alta, bairros) e pacientes autenticados favoritam, contatam e veem economia | erro < 1%, busca/detalhe/clínica p95 < 300 ms, p99 da busca < 800 ms |
| `stress` | Taxa de requisições de 50 até 1.600/s, sem pausas | erro < 5%, p95 < 1,5 s (observação: busca o limite) |
| `spike` | 10 → 150 usuários em 10 s, depois volta a 10 | erro < 5%; a latência volta ao normal |
| `soak` | 20 usuários por 10 min ou mais | busca p95 < 300 ms; memória sem crescer |
| `latency` | 5 usuários só lendo, via domínio público | erro < 1%, busca p95 < 500 ms (inclui rede) |

A busca usa termos reais, incluindo nomes populares ("colesterol", "chapa do braço") e frases, com filtros aleatórios de bairro, preço e nota.

## Resultados (13/10/2026)

Ambiente espelho: 2 CPUs, API com 600 MB (JVM com serial GC e heap de 65%), PostgreSQL com 200 MB. O gerador de carga roda na mesma máquina e disputa CPU com a API, então os números são conservadores.

| Cenário | Requisições | Taxa média | Latência (média / p95 / máx) | Erros |
|---|---|---|---|---|
| smoke | 131 | 4/s | 37 ms / 99 ms / 1,5 s (partida a frio) | 0% |
| load | 7.391 | 43/s | 2,8 ms / 6,7 ms / 1,0 s | 0% |
| load, só a busca | — | — | p95 **4,1 ms**, p99 6,2 ms | 0% |
| stress (até 1.600/s) | 156.861 | 713/s | 5,8 ms / 14,7 ms / 470 ms | 0% |
| spike (150 usuários) | 16.269 | 95/s | 1,5 ms / 2,4 ms / 12 ms | 0% |
| soak (4 min, 20 usuários) | 3.164 | 13/s | 1,8 ms / 2,5 ms / 10 ms | 0% |
| **produção** via Cloudflare | 472 | 11/s | 99 ms / 295 ms / 922 ms | 0% |
| **produção**, só a busca | — | — | mediana **54 ms**, p95 **227 ms** | 0% |

### Leitura dos resultados

- **RNF03 atendido** com grande folga: a busca responde em milissegundos na API; os ~50 ms de mediana em produção são rede, Cloudflare e túnel.
- **Capacidade:** a API sustentou cerca de **700 requisições/s em média** durante a rampa, com picos de 1.200 a 1.600/s, sem um único erro. A CPU só passou de 100% (de 200%) no final; o k6 também usa CPU na mesma máquina. Para uma cidade como Surubim, isso é muito acima da necessidade.
- **Memória estável:** 400 MiB do início ao fim da resistência, ou seja, sem vazamento. O heap fica limitado e o container tem 200 MB de folga.
- **Recuperação:** após o pico de 150 usuários a latência volta ao normal imediatamente.
- **Gargalo esperado:** cadastro e login usam BCrypt (custo 10), que consome CPU de propósito (segurança). Eles não foram saturados nestes testes. Se o app tiver picos de cadastro, o limite virá daí.

## Ajustes de desempenho aplicados na VPS

A VPS (2 CPUs, ~1 GB de RAM) ficou dedicada à API depois da limpeza dos outros projetos.

| Ajuste | Onde |
|---|---|
| API com 600 MB, JVM com `-XX:+UseSerialGC`, heap de 65%, `Xss512k`, metaspace limitado e `ExitOnOutOfMemoryError` | `compose.prod.yaml` |
| PostgreSQL com 200 MB: `shared_buffers=64MB`, `effective_cache_size=192MB`, `random_page_cost=1.1`, `jit=off`, `max_connections=30` | `compose.prod.yaml` |
| Pool de conexões de 10, Tomcat com até 100 threads, compressão de JSON, JMX desligado | `application.yaml` |
| Rotação de logs do Docker (10 MB × 3) para não encher o disco | `compose.prod.yaml` |
| `vm.swappiness=10`, `somaxconn=1024` | `/etc/sysctl.d/99-clinics.conf` na VPS |
| Serviços inúteis desligados (fwupd, udisks2, rpcbind) e projetos antigos removidos (disco de 70% para 30%) | VPS |

## Limites desta medição

- O ambiente espelho tem os mesmos limites de CPU e memória da VPS, mas o disco e o processador reais podem diferir.
- O teste em produção é só de leitura e leve de propósito, para não derrubar a VPS nem poluir o banco.
- Não medimos a carga de escrita pesada (muitos cadastros por segundo).
