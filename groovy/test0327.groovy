node('HID5272N.automotive-wan.com-ssh'){
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

timestamps{
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
                        
                
                        # 拷贝源目录的py 文件到目标目录
                        cp -f "${sourceDir}"/*.py "${targetDir}/"

                        # 拷贝 app_template-gi 文件夹到目标目录
                        cp -rf "${sourceDir}/app_template" "${targetDir}/"
                    """
    }
}
}
