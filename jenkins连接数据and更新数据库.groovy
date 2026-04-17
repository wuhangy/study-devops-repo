// 定义函数：update_db_sonix，接收一个 Map 类型的数据 sonixData，以及可选的数据库 URL
def update_db_sonix(Map sonixData, String dbUrl = "${env.couchdb_cmb}") {
    // 使用 retry 块，如果内部代码执行失败，会自动重试最多 5 次，提高流水线稳定性
    retry(5) {
        // 使用 httpRequest 插件向 CouchDB 发起 GET 请求，获取对应 buildid 的文档内容
        def response = httpRequest(
            url: "${dbUrl}/${buildid}",            // 拼接数据库地址和文档 ID
            acceptType: 'APPLICATION_JSON',        // 要求返回 JSON 格式数据
            ignoreSslErrors: true,                 // 忽略 HTTPS 证书错误
            validResponseCodes: '200,404'          // 允许返回 200 (存在) 或 404 (不存在，代表文档未创建)
        )
        
        // 判断文档是否存在：若返回 200，则读取内容并转为对象；若返回 404，则初始化一个仅包含 ID 的 Map
        def currentDoc = (response.status == 200) ? readJSON(text: response.content) : ["_id": "${buildid}"]
        
        // 定义根键名，格式为“MCU名称_变体”，作为文档中的主分类
        def rootKey = "${mcuName}_${variant}"

        // 如果该 rootKey 不存在，则初始化一个空的嵌套 Map
        if (!currentDoc[rootKey]) currentDoc[rootKey] = [:]
        // 如果该 rootKey 下的 sonix_data 不存在，则初始化一个空的嵌套 Map
        if (!currentDoc[rootKey]["sonix_data"]) currentDoc[rootKey]["sonix_data"] = [:]
        
        // 将 buildid、mcu、variant 等元数据填入 sonix_data 节点，确保类型强制转为 String
        currentDoc[rootKey]["sonix_data"]["buildid"] = "${buildid}".toString()
        currentDoc[rootKey]["sonix_data"]["mcu"]     = "${mcuName}".toString()
        currentDoc[rootKey]["sonix_data"]["variant"] = "${variant}".toString()
        currentDoc[rootKey]["sonix_data"]["carline"] = "".toString() // 初始化 carline 为空字符串
        
        // 遍历传入的 sonixData Map，将其中的数据合并到文档中
        sonixData.each { module, pathMap ->
            // 如果传入的数据项是一个 Map（嵌套结构），则遍历该 Map 并将其内部的值全部转换为字符串
            if (pathMap instanceof Map) {
                currentDoc[rootKey]["sonix_data"][module] = pathMap.collectEntries { k, v -> 
                    [k, v?.toString()] // 使用 collectEntries 将键值对放入新 Map，确保值是 String
                }
            } else {
                // 如果不是 Map，则直接将其转换为字符串存入
                currentDoc[rootKey]["sonix_data"][module] = pathMap?.toString()
            }
        }

        // 调用 CouchDB 的 Update Handler（设计文档中的自定义函数）来提交最终的更新
        httpRequest(
            url: "${dbUrl}/_design/example/_update/add_timestamp/${buildid}", // 调用特定的 _update 处理接口
            httpMode: 'POST',                                                  // 使用 POST 方法提交数据
            contentType: 'APPLICATION_JSON',                                   // 指定发送的内容类型为 JSON
            requestBody: groovy.json.JsonOutput.toJson(currentDoc),            // 将更新后的 currentDoc 对象转为 JSON 字符串
            ignoreSslErrors: true                                              // 再次忽略 SSL 错误
        )
    }
}

///更新数据库

update_db_sonix([
                                "mapviewer": [
                                                "excel": "${serverFolder}\\mapviewer\\${excelFiles_Mapviewer}",
                                                "xml": "${serverFolder}\\mapviewer\\jenkins_exporter_results_project.xml"
                                            ]
                                    ])



 update_db_sonix([
                                        "build_warning": [
                                        "excel": "${serverFolder}\\warning_reports\\${excelFiles_warning_reports}",
                                        "xml": "${serverFolder}\\warning_reports\\jenkins_exporter_results_project.xml"
                                                        ]
                      
def update_db() {
    retry(5) {
        // 1. 先尝试获取旧文档，防止覆盖掉已有字段
        def response = httpRequest(
            url: "${couchdb_cmb}/${buildid}",
            acceptType: 'APPLICATION_JSON',
            ignoreSslErrors: true,
            validResponseCodes: '200,404'
        )

        // 2. 如果存在旧文档则读取，不存在则创建一个新的空 Map
        def B = (response.status == 200) ? readJSON(text: response.content) : ["_id": "${buildid}"]
        
        // 3. 将新信息合并进入文档 (使用赋值，确保 pipeline_info 更新进去)
        B["${mcuName}_${variant}"] = pipeline_info

        // 4. 发送合并后的数据
        httpRequest(
            acceptType: 'APPLICATION_JSON', 
            contentType: 'APPLICATION_JSON',
            httpMode: 'POST',
            ignoreSslErrors: true,
            requestBody: groovy.json.JsonOutput.toJson(B),
            url: "${couchdb_cmb}/_design/example/_update/add_timestamp/${buildid}"
        )
        
        echo "Successfully updated database for build: ${buildid}"
    }
    // 成功后休息一下，避免对数据库造成压力
    sleep(time: 5, unit: "SECONDS")
}
