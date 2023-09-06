# Elasticsearch 备份和恢复工具手册

## 概览

Elasticsearch 备份和恢复工具是一个基于 Java 的工具，旨在高效地将 Elasticsearch 索引中的文档备份到 JSON 文件中，并将它们恢复到相同或不同的 Elasticsearch 集群中。该工具利用 Elasticsearch 的 Scroll API 以资源高效的方式处理大型数据集。

该工具支持命令行参数和配置文件两种模式，一旦指定了配置文件路径，则参数仅从配置文件中读取。

## 先决条件

1. 系统上应安装 Java 8 或更高版本。
2. 可访问的、正在运行的 Elasticsearch 集群。

## 打包方式
- Gradle
在根目录下执行
```bash
./gradlew clean :es-data-transfer-tool:packageZip
```
打包成果物在 `/es-data-transfer-tool/build/dist/es-data-transfer-tool-distribution-1.1.0.zip`

- Maven
进入 /es-data-transfer-tool 执行
```bash
cd /es-data-transfer-tool
mvn install
```
打包成果物在`/es-data-transfer-tool/target`目录下

## 命令行参数

该工具接受以下命令行参数：

- `c` 或 `-config` （**可选**）：使用配置文件执行操作，一旦选择，后续所有参数将无效，请确保配置文件书写正确
- `a` 或 `-action`（**必填**）：要执行的操作，`backup`（备份）或 `restore`（恢复）
- `e` 或 `-endpoint`（**必填**）：Elasticsearch 端点 URL（例如，`http://localhost:9200`）
- `f` 或 `-filePath`（**必填**）：保存备份或读取恢复的文件路径（例如，`/path/to/backup.json`）
- `i` 或 `-indexName`（**必填**）：要备份或恢复的索引名称
- `q` 或 `-dslQuery`（**可选**）：备份的 DSL 查询，如果您想过滤要备份的文档。注意在单引号中书写查询体； 如果使用双引号，请在查询体中的双引号前使用 "\\"；如果是 windows,则使用 "\\\\"
- `u` 或 `-username`（**可选**）：Elasticsearch 用户名，用于身份验证
- `p` 或 `-password`（**可选**）：Elasticsearch 密码，用于身份验证

## 使用示例

### 默认备份

- 该示例执行 `my_index_name` 索引中所有文档的备份：
```bash
# Basic
java -jar es-data-transfer-tool-1.1.0.jar -a backup -e http://localhost:9200 -f my_index-backup.json -i my_index_name

# Authentication
java -jar es-data-transfer-tool-1.1.0.jar -a backup -e http://localhost:9200 -f my_index-backup.json -i my_index_name -u my_user -p my_password
```

### 基于查询的备份

- 在此示例中，只有与查询匹配的文档将从 `my_index` 索引中备份：
```bash
# Linux
java -jar es-data-transfer-tool-1.1.0.jar -a backup -e http://localhost:9200 -f my_index-backup.json -i my_index_name -q '{"query":{"range":{"structTime":{"gte":"2023-01-11T00:10:00.000+08:00","lte":"2023-02-07T22:50:00.000+08:00"}}}}'

# Or
java -jar es-data-transfer-tool-1.1.0.jar -a backup -e http://localhost:9200 -f my_index-backup.json -i my_index_name -q "{\"query\":{\"range\":{\"structTime\":{\"gte\":\"2023-01-11T00:10:00.000+08:00\",\"lte\":\"2023-02-07T22:50:00.000+08:00\"}}}}"
```

 ### 恢复

- 恢复到集群，鉴权和不带鉴权：
```bash
# Basic
java -jar es-data-transfer-tool-1.1.0.jar -a restore -e http://localhost:9200 -f my_index-backup.json -i new_index

# Authentication 
java -jar es-data-transfer-tool-1.1.0.jar -a restore -e http://localhost:9200 -f my_index-backup.json -i new_index -u my_user -p my_password
```

### 使用配置文件执行备份和恢复

- 配置文件为 json 格式，字段与命令行参数（完整）一致；
- 跟据操作需求修改配置文件

- 执行如下命令 `java -jar es-data-transfer-tool-1.1.0.jar --config ${path_to_config_file}`，如：
```bash
java -jar es-data-transfer-tool-1.1.0.jar --config ./transfer-config.json
```

- 配置文件示例，可在附件中查看，注意修改 “action” 指定备份 (backup) 还是恢复 (restore)
```json
{
  "action":  "backup",
  "endpoint": "localhost:9200",
  "username": "admin",
  "password": "admin",
  "indexName":  "my_index_name",
  "filePath":  "./my_index-backup.json",
  "dslQuery": {
    "query": {
      "range": {
        "structTime": {
          "gte": "2023-01-11T00:00:00.000+08:00",
          "lte": "2023-01-21T00:00:00.000+08:00"
        }
      }
    }
  }
}
```

### 使用 `nohub` 后台运行
- 当备份或恢复时间过长时，需要使用`nohup`后台运行，否则终端断开时，备份会终端，导致数据缺失
- 方法为: `nouhp {your_backup_or_resotre_cmd} &`
```
# Backup
nohup java -jar es-data-transfer-tool-1.1.0.jar -a backup -e http://localhost:9200 -f my_index-backup.json -i my_index_name &

# Restore
nohup java -jar es-data-transfer-tool-1.1.0.jar -a restore -e http://localhost:9200 -f my_index-backup.json -i new_index & 

```

## 错误信息和故障排除

- **"缺少必填参数"**：确保提供了所有必需的参数，(-a -e -f -i)，或在配置文件中保证相关参数存在。
- **"要备份的索引不存在"**：请确认你想要备份的索引确实存在，如果不存在会提示索引不存在。
- **"备份文件不存在或无法读取"**：检查文件路径及其权限，为了避免错误覆盖，如果当要备份的文件存在，则无法执行，请先删除或转移后重新执行。
- **"待恢复（导入）的索引存在"**：为了避免污染集群中的索引，当待导入索引存在时，无法执行，请修改导入索引名，或删除集群中的索引名后，重新执行。
- **"鉴权"**：当集群存在鉴权时，请正确填写用户名和密码（-u -p）。

## 注意事项

1. **JAVA 环境**：请确保有 java 1.8 既以上的运行环境。
2. **索引版本**：在使用此工具进行备份和恢复操作时，确保源索引和目标索引的 Elasticsearch 版本兼容。
3. **备份文件大小**：备份大型数据集时，确保有足够的磁盘空间来存储备份文件。
4. **网络连接**：确保源和目标 Elasticsearch 集群都是可访问的，并且网络连接是稳定的。
5. **权限**：如果 Elasticsearch 集群启用了安全性，则确保提供正确的用户名和密码。
6. **查询验证**：在使用基于查询的备份选项时，务必验证您的 DSL 查询是有效的。
7. **恢复到现有索引**：如果尝试恢复到一个已存在的索引，操作将被终止。确保目标索引不存在或愿意覆盖它。


## 备注

如果导入速度过慢，可尝试用以下脚本进行导入。 **注意该方法使用不当会造成集群被污染，或污染执行目录，请确保知道自己在干什么，如果不追求导入速度，使用上述方法导入即可。**

**使用方法**：

- 修改索引名称、地址和鉴权信息。
- **索引名一定要是集群中不存在的索引，不然会污染集群。**
- 保存脚本，如 bulk_index.sh。

```bash
SPLIT_PATTERN="s-*-bulk.*"
  
ES_HOST=localhost
ES_PORT=9200
ES_INDEX=my_index_name-restore-shell

# sed -i 's/\(\"_id\":\)\([ ]*\)\([a-z0-9-]*[^}^\s*]\)/\1\"\3\"/' demo.txt
if [ -z "$1" ]; then
  echo "Please input files..."
  exit 1
fi

for var in "$@"
do
  split -10000 "${var}" s-"${var}"-bulk.
done

if [ "$?" -ne "0" ]; then
  rm -f ${SPLIT_PATTERN}
  exit 1
fi

for var in ${SPLIT_PATTERN}
do
  echo "curl -H \"Content-Type: application/x-ndjson\" -XPOST \"http://${ES_HOST}:${ES_PORT}/${ES_INDEX}/_bulk?pretty\" --data-binary \"@${var}\""
  curl -H "Content-Type: application/x-ndjson" -XPOST "http://${ES_HOST}:${ES_PORT}/${ES_INDEX}/_bulk?pretty" --data-binary "@${var}" 1> /dev/null 2>&1
done

rm -f ${SPLIT_PATTERN}
```

- 给上述脚本命名为 `bulk_index.sh`，
- 给予写权限 `chmod +x bulk_index.sh`，
- 在有备份文件的位置执行。
**注意，索引名一定要是集群中不存在的索引，不然会污染集群，请再次确认。**
```bash
./bulk_index.sh my_index-backup.json
```

