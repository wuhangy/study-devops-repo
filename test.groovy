node('win'){
    // 定义参数
    properties([
        parameters([
            string(
                name: 'SourceDir',  
                defaultValue: 'C:/Users/uih54899/Desktop/New folder/test_1',  
                description: 'source', 
                trim: true  
            ),
            string(
                name: 'TargetDir',
                defaultValue: 'C:/Users/uih54899/Desktop/New folder/test_2/new_files',
                description: 'target',
                trim: true
            )
        ])
    ])

    timestamps {
        // --- 新增：从 GitHub 拉取代码阶段 ---
        stage('Checkout') {
            checkout([$class: 'GitSCM', 
                branches: [[name: '*/main']], 
                userRemoteConfigs: [[
                    credentialsId: 'wuhagy-token', 
                    url: 'https://github.com/wuhangy/study-devops-repo.git'
                ]]
            ])
        }

        stage("Init"){
            manager.addShortText("node: ${env.NODE_NAME}","black", "lightgreen","0px", "white")
            manager.addShortText("workspace: ${env.WORKSPACE}","black", "lightgreen","0px", "white")
            manager.addShortText("source: ${SourceDir}","black", "lightgreen","0px", "white")
            manager.addShortText("target: ${TargetDir}","black", "lightgreen","0px", "white")
        }

        stage('move-files') {
            def sourceDir = params.SourceDir
            def targetDir = params.TargetDir

            sh """
                #!/bin/bash
                # 如果目标目录不存在，先创建 
                if [ ! -d "${targetDir}" ]; then
                    mkdir -p "${targetDir}"
                fi
                
                # 拷贝源目录的 py 文件到目标目录
                # 注意：这里是从你本地指定的 SourceDir 拷贝，而不是从刚下载的 GitHub 代码库拷贝
                cp -f "${sourceDir}"/*.py "${targetDir}/"

                # 拷贝 app_template 文件夹到目标目录
                cp -rf "${sourceDir}/app_template" "${targetDir}/"
            """
        }
    }
}
