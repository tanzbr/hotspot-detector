# Hotspot Detector - Sistema Simplificado

Sistema de monitoramento de Access Points Wi-Fi com persistência em MariaDB.

## Funcionalidades

1. **Monitoramento em Tempo Real**: Exibe tabela atualizada automaticamente a cada 1 minuto com os hotspots detectados
2. **Pesquisa por Data**: Consulta redes disponíveis em períodos específicos
3. **Persistência Automática**: Dados salvos automaticamente no MariaDB

## Pré-requisitos

- Java 17+
- MariaDB Server 11.4.5+
- Maven 3.6+
- Privilégios de administrador (para escaneamento Wi-Fi)

## Configuração Rápida

### 1. Banco de Dados

```bash
# Instalar MariaDB
sudo apt install mariadb-server  # Ubuntu/Debian
# ou
brew install mariadb             # macOS

# Configurar banco
sudo mysql
CREATE DATABASE hotspot_detector;
CREATE USER 'hotspot_user'@'localhost' IDENTIFIED BY 'hotspot_pass';
GRANT ALL PRIVILEGES ON hotspot_detector.* TO 'hotspot_user'@'localhost';
FLUSH PRIVILEGES;
```

### 2. Configuração

**Opção 1 - Usar configuração padrão:**
O sistema funciona sem configuração adicional usando:
- Host: localhost:3306
- Banco: hotspot_detector
- Usuário: hotspot_user
- Senha: hotspot_pass

**Opção 2 - Personalizar configuração:**
```bash
cp src/main/resources/database.example.yml src/main/resources/database.yml
```

Edite `database.yml`:
```yaml
database:
  host: localhost
  port: 3306
  name: hotspot_detector
  username: hotspot_user
  password: hotspot_pass
```

### 3. Execução

```bash
# Compilar
mvn clean package

# Executar (como administrador)
sudo java -jar target/hotspot-detector-1.0.0.jar
```

## Menu Principal

```
1. Monitoramento em tempo real
   - Atualização automática a cada 1 minuto
   - Pressione ENTER para atualizar manualmente
   - Digite 'voltar' para sair

2. Pesquisar data específica
   - Formato: dd/MM/yyyy HH:mm
   - Exemplo: 15/12/2024 14:30

3. Sair
```

## Estrutura do Banco

A tabela `access_points` armazena:
- SSID (Nome da Rede)
- MAC Address
- Qualidade do Link (%)
- Nível de Sinal (dBm)
- Canal
- Frequência (GHz)
- Informações de beacon
- Versão de segurança
- Data/hora do escaneamento

## Logs

Os logs são salvos em `logs/hotspot-detector.log` com rotação automática.

## Solução de Problemas

### Erro de Permissão
```bash
# Execute como administrador
sudo java -jar target/hotspot-detector-1.0.0.jar
```

### Erro de Conexão com Banco
- Verifique se o MariaDB está rodando
- Confirme as credenciais em `database.yml`
- Teste a conexão: `mysql -u hotspot_user -p hotspot_detector`

### Nenhum Access Point Detectado
- Verifique se o Wi-Fi está habilitado
- Confirme se há redes Wi-Fi próximas
- No Linux, instale: `sudo apt install wireless-tools`

## Arquitetura Simplificada

```
Main.java
├── AccessPointPersistenceService (escaneamento + persistência)
├── AccessPointScheduler (execução automática)
└── AccessPointRepository (operações de banco)
```

O sistema inicia automaticamente o escaneamento a cada 1 minuto e mantém os dados persistidos no MariaDB para consultas históricas. 