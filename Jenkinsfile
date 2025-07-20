// Aşama 4: Modrinth'e Yayınla (Sadece master/main branch için)
stage('Publish to Modrinth') {
    when {
        expression {
            return env.GIT_BRANCH == 'origin/main' || env.GIT_BRANCH == 'origin/master' || env.GIT_BRANCH == 'main' || env.GIT_BRANCH == 'master'
        }
    }
    steps {
        echo 'Modrinth\'e yayınlanıyor...'
        withCredentials([string(credentialsId: 'MODRINTH_TOKEN', variable: 'MODRINTH_API_TOKEN')]) {
            script {
                def jdkHome = "${env.WORKSPACE}/${JDK_DIR_NAME}"
                def mavenHome = tool MAVEN_TOOL_NAME

                // Maven'ı PATH'e ekleyerek versiyonu oku
                withEnv([
                    "JAVA_HOME=${jdkHome}",
                    "PATH=${mavenHome}/bin:${jdkHome}/bin:${env.PATH}",
                    "M2_HOME=${mavenHome}"
                ]) {
                    // pom.xml'den versiyonu oku
                    def pomVersion = sh(returnStdout: true, script: 'mvn help:evaluate -Dexpression=project.version -q -DforceStdout').trim()
                    def versionNumber = "${pomVersion}"
                    def fileName = sh(returnStdout: true, script: 'basename ${JAR_FILE_PATH}').trim()

                    echo "Yayınlanacak versiyon: ${versionNumber}"
                    echo "Dosya adı: ${fileName}"

                    // Changelog'u commit mesajından oluştur
                    def changelog = env.COMMIT_MESSAGE ?: "Yeni sürüm: ${versionNumber}"
                    // JSON için özel karakterleri escape et
                    def escapedChangelog = changelog.replaceAll('"', '\\\\"').replaceAll('\\n', '\\\\n').replaceAll('\\r', '')

                    echo "Changelog: ${escapedChangelog}"

                    // JSON payload'u dosyaya yaz
                    def jsonPayload = """
{
    "name": "${versionNumber}",
    "version_number": "${versionNumber}",
    "changelog": "${escapedChangelog}",
    "dependencies": [],
    "game_versions": ${MINECRAFT_VERSIONS},
    "version_type": "release",
    "loaders": ${LOADERS},
    "featured": false,
    "project_id": "${MODRINTH_PROJECT_ID}"
}
"""
                    writeFile file: 'modrinth_payload.json', text: jsonPayload

                    // JSON dosyasını kontrol et
                    sh 'cat modrinth_payload.json'

                    // İlk olarak dosya var mı kontrol et
                    sh "ls -la ${env.JAR_FILE_PATH}"
                    sh "file ${env.JAR_FILE_PATH}"

                    // Modrinth API'ye yayınla - daha detaylı hata ayıklama
                    def response = sh(returnStdout: true, script: '''
                        curl -v -w "\\nHTTPSTATUS:%{http_code}" -X POST 'https://api.modrinth.com/v2/version' \\
                            -H "Authorization: ''' + env.MODRINTH_API_TOKEN + '''" \\
                            -H "User-Agent: Jenkins/ATOMLAND-Optimizer" \\
                            -F "data=@modrinth_payload.json;type=application/json" \\
                            -F "file=@''' + env.JAR_FILE_PATH + '''"
                    ''').trim()

                    echo "Full curl response:"
                    echo response

                    // HTTP status kodunu kontrol et
                    def lines = response.split('\n')
                    def httpStatusLine = lines.find { it.startsWith('HTTPSTATUS:') }
                    def httpStatus = httpStatusLine ? httpStatusLine.split(':')[1] : 'unknown'
                    def responseBody = response.replaceAll(/.*HTTPSTATUS:\d+/, '').trim()

                    echo "HTTP Status: ${httpStatus}"
                    echo "Response Body: ${responseBody}"

                    if (httpStatus.startsWith("2")) {
                        echo "✅ Modrinth'e başarıyla yayınlandı!"
                    } else {
                        // Alternatif API çağrısı deneyelim
                        echo "⚠️ İlk deneme başarısız, alternatif yöntem deneniyor..."

                        // JSON içeriğini değişken olarak sakla (dosyayı silmeden önce)
                        def jsonContent = jsonPayload.replaceAll('\n', '').replaceAll('\\s+', ' ').trim()

                        def alternativeResponse = sh(returnStdout: true, script: '''
                            curl -X POST 'https://api.modrinth.com/v2/version' \\
                                -H "Authorization: ''' + env.MODRINTH_API_TOKEN + '''" \\
                                -H "User-Agent: Jenkins/ATOMLAND-Optimizer" \\
                                -H "Content-Type: multipart/form-data" \\
                                --form 'data=''' + jsonContent + ''';type=application/json' \\
                                --form 'file=@''' + env.JAR_FILE_PATH + ''';filename=''' + fileName + ''';type=application/java-archive'
                        ''').trim()

                        echo "Alternative response: ${alternativeResponse}"

                        if (alternativeResponse.contains('"id"')) {
                            echo "✅ Alternatif yöntemle Modrinth'e başarıyla yayınlandı!"
                        } else {
                            error "❌ Modrinth yayınlama hatası! HTTP Status: ${httpStatus}, Response: ${responseBody}, Alternative: ${alternativeResponse}"
                        }
                    }

                    // Geçici dosyayı temizle (en son yapılacak işlem)
                    sh 'rm -f modrinth_payload.json'
                }
            }
        }
    }
}