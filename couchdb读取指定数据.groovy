// 1. 加载共享库
@Library("portal-jenkins-lib@main") _
@Library("metric@main") _metric

node("HID5271W.automotive-wan.com-ssh") {
    // 定义变量
    def couchdb_cmb = "https://marshal.cn3012.aws-cnno1.continental.cloud/apisix/couchdb-chy26/cmb"


    properties([
    parameters([
        choice( choices: ['ZCUD', 'ZCUP'], description: 'MCU name', name: 'mcuName'),
        string( defaultValue: '', description: 'variant', name: 'variant', trim: true),
        string( defaultValue: '', description: 'buildid', name: 'buildid', trim: true)
    ])
])
    // //def mcuName = "ZCUD"
    // def variant = "T13T_BEV"
    // def buildid = "20260417_00_nightly"
    def rootKey = "${mcuName}_${variant}"
    
    // 指定保存路径
    // 注意：在 Groovy 中路径要使用双反斜杠 \\ 或者 正斜杠 /
    def targetFolder = "C:\\Users\\uih54899\\Desktop\\New folder"
    def targetFile = "${targetFolder}\\.json"

    try {
        stage("Fetch Data") {
            def response = httpRequest(
                url: "${couchdb_cmb}/${buildid}",
                ignoreSslErrors: true,
                contentType: 'APPLICATION_JSON'
            )
            resJSON = readJSON text: response.content
        }

        stage("Save to Local Path") {
            def allSonixData = resJSON[rootKey]?.sonix_data
            
            if (allSonixData instanceof Map) {
                echo ">>> 准备保存数据到桌面文件夹 <<<"
                
                // 确保文件夹存在（可选，如果是固定存在的桌面文件夹可以跳过）
                bat "if not exist \"${targetFolder}\" mkdir \"${targetFolder}\""

                // 使用 writeJSON 将 Map 写入指定位置
                // writeJSON 支持完整路径
                writeJSON file: targetFile, json: allSonixData, pretty: 4
                
                echo "成功！数据已保存至: ${targetFile}"
            } else {
                error "未找到指定的 sonix_data 节点，无法保存。"
            }
        }

        stage("Verify") {
            // 在控制台打印文件前 10 行确认
            bat "powershell -Command \"Get-Content '${targetFile}' | Select-Object -First 10\""
        }

    } catch (Exception e) {
        currentBuild.result = 'FAILURE'
        echo "发生错误: ${e.message}"
        
        emailext(
            subject: "JSON Save Failed: ${buildid}",
            body: "无法将 JSON 保存到路径: ${targetFile}\n错误详情: ${e.message}",
            to: "hangyin.2.wu-EXT@aumovio.com"
        )
        throw e
    }
}