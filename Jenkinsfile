// Jenkins Pipeline Script for ATOMLAND Optimizer
// GitHub webhook ile otomatik build tetikleme + Modrinth yayınlama
pipeline {
    // 1. Agent Ayarı
    agent any

    // 2. Ortam Değişkenleri
    environment {
        MAVEN_TOOL_NAME = 'Maven 3.9.6'
        JDK_URL = "https://github.com/adoptium/temurin21-binaries/releases/download/jdk-21.0.4%2B7/OpenJDK21U-jdk_x64_linux_hotspot_21.0.4_7.tar.gz"
        JDK_DIR_NAME = 'jdk-21'

        // Modrinth ayarları
        MODRINTH_PROJECT_ID = 'dMkSe22y' // ATOMLAND Optimizer project ID
        MINECRAFT_VERSIONS = '["1.20.5", "1.20.6", "1.21", "1.21.1", "1.21.2", "1.21.3", "1.21.4", "1.21.5", "1.21.6", "1.21.7", "1.21.8"]'
        LOADERS = '["paper", "purpur", "spigot", "bukkit"]'
    }

    // 3. Tetikleyiciler - GitHub webhook ve SCM polling
    triggers {
        // GitHub webhook tetikleyicisi (ana tetikleyici)
        githubPush()
        // Yedek olarak SCM polling (webhook çalışmadığı durumlar için)
        pollSCM('H/5 * * * *') // Her 5 dakikada bir kontrol eder
    }

    // 4. Pipeline Seçenekleri
    options {
        // Build geçmişini sınırla
        buildDiscarder(logRotator(numToKeepStr: '10'))
        // Eş zamanlı build'leri önle
        disableConcurrentBuilds()
        // Zaman aşımı ayarla
        timeout(time: 30, unit: 'MINUTES')
        // GitHub durumunu güncelle
        githubProjectProperty(projectUrlStr: 'https://github.com/ATOMGAMERAGA/ATOMLAND-Optimizer')
    }

    // 5. Aşamalar
    stages {
        // Aşama 0: Git Checkout (Önemli!)
        stage('Checkout') {
            steps {
                echo "GitHub repository'den kod çekiliyor..."
                checkout scm
                script {
                    // Son commit bilgilerini göster
                    def commitId = sh(returnStdout: true, script: 'git rev-parse HEAD').trim()
                    def commitMessage = sh(returnStdout: true, script: 'git log -1 --pretty=%B').trim()
                    def branchName = sh(returnStdout: true, script: 'git rev-parse --abbrev-ref HEAD').trim()
                    echo "Build tetikleyen commit: ${commitId}"
                    echo "Branch: ${branchName}"
                    echo "Commit mesajı: ${commitMessage}"

                    // Global değişkenleri ayarla
                    env.COMMIT_MESSAGE = commitMessage
                    env.BRANCH_NAME = branchName
                    env.GIT_COMMIT = commitId
                }
            }
        }

        // Aşama 1: Özel JDK Kurulumu
        stage('Install Custom JDK') {
            steps {
                echo "Özel JDK 21 kurulumu başlatılıyor..."
                sh """
                    set -e
                    if [ -d "${JDK_DIR_NAME}" ]; then
                        echo "JDK zaten kurulu: ${JDK_DIR_NAME}"
                    else
                        echo "JDK indiriliyor: ${JDK_URL}"
                        wget --quiet --timeout=60 --tries=3 -O jdk.tar.gz "${JDK_URL}"
                        echo "Arşiv açılıyor..."
                        mkdir -p "${JDK_DIR_NAME}"
                        tar -xzf jdk.tar.gz -C "${JDK_DIR_NAME}" --strip-components=1
                        echo "İndirilen arşiv dosyası temizleniyor..."
                        rm jdk.tar.gz
                    fi
                    echo "JDK kurulumu başarıyla tamamlandı."
                """
            }
        }

        // Aşama 2: Projeyi Maven ile Derle
        stage('Build with Maven') {
            steps {
                script {
                    def jdkHome = "${env.WORKSPACE}/${JDK_DIR_NAME}"
                    def mavenHome = tool MAVEN_TOOL_NAME

                    // PATH'i doğru şekilde ayarla
                    withEnv([
                        "JAVA_HOME=${jdkHome}",
                        "PATH=${mavenHome}/bin:${jdkHome}/bin:${env.PATH}",
                        "M2_HOME=${mavenHome}"
                    ]) {
                        echo "Kullanılan Java sürümü kontrol ediliyor..."
                        sh 'java -version'
                        echo "Kullanılan Maven sürümü kontrol ediliyor..."
                        sh 'mvn -v'

                        // Maven komutlarını ayrı ayrı çalıştır
                        echo "Proje temizleniyor..."
                        sh 'mvn clean'

                        echo "Proje Maven ile derleniyor... Build Numarası: ${env.BUILD_NUMBER}"
                        echo "Git Branch: ${env.GIT_BRANCH}"
                        sh 'mvn package -DskipTests=false'

                        // JAR dosyasının tam yolunu bul ve global değişkene kaydet
                        def jarFile = sh(returnStdout: true, script: 'find target -name "*.jar" -type f | head -1').trim()
                        if (jarFile) {
                            env.JAR_FILE_PATH = jarFile
                            echo "JAR dosyası bulundu: ${jarFile}"

                            // JAR dosyasının boyutunu ve hash'ini göster
                            sh """
                                echo "JAR dosya bilgileri:"
                                ls -lh ${jarFile}
                                echo "MD5 Hash: \$(md5sum ${jarFile} | cut -d' ' -f1)"
                            """
                        } else {
                            error "JAR dosyası bulunamadı!"
                        }
                    }
                }
            }
            // Test sonuçlarını yayınla
            post {
                always {
                    // JUnit test sonuçları varsa yayınla
                    script {
                        if (fileExists('target/surefire-reports/*.xml')) {
                            junit 'target/surefire-reports/*.xml'
                        }
                    }
                }
            }
        }

        // Aşama 3: Oluşturulan Dosyayı Arşivle
        stage('Archive Artifact') {
            steps {
                echo 'Oluşturulan .jar dosyası arşivleniyor...'
                script {
                    // JAR dosyasının var olup olmadığını kontrol et
                    def jarFiles = sh(returnStdout: true, script: 'find target -name "*.jar" -type f').trim()
                    if (jarFiles) {
                        echo "Bulunan JAR dosyaları: ${jarFiles}"
                        archiveArtifacts artifacts: 'target/*.jar', fingerprint: true, allowEmptyArchive: false
                    } else {
                        error "JAR dosyası bulunamadı!"
                    }
                }
            }
        }

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
                            writeFile file: 'modrinth_payload.json', text: """
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

                            // JSON dosyasını kontrol et
                            sh 'cat modrinth_payload.json'

                            // Modrinth API'ye yayınla - güvenli yöntem
                            def response = sh(returnStdout: true, script: '''
                                curl -s -w "HTTPSTATUS:%{http_code}" -X POST 'https://api.modrinth.com/v2/version' \\
                                    -H "Authorization: ''' + env.MODRINTH_API_TOKEN + '''" \\
                                    -F "data=@modrinth_payload.json;type=application/json" \\
                                    -F "file=@''' + env.JAR_FILE_PATH + ''';filename=''' + fileName + '''"
                            ''').trim()

                            // Geçici dosyayı temizle
                            sh 'rm -f modrinth_payload.json'

                            // HTTP status kodunu kontrol et
                            def httpStatus = response.tokenize("HTTPSTATUS:")[1]
                            def responseBody = response.tokenize("HTTPSTATUS:")[0]

                            echo "HTTP Status: ${httpStatus}"
                            echo "Response: ${responseBody}"

                            if (httpStatus.startsWith("2")) {
                                echo "✅ Modrinth'e başarıyla yayınlandı!"
                            } else {
                                error "❌ Modrinth yayınlama hatası! HTTP Status: ${httpStatus}, Response: ${responseBody}"
                            }
                        }
                    }
                }
            }
        }

        // Aşama 5: Build Bildirim
        stage('Notification') {
            steps {
                echo "Build başarıyla tamamlandı!"
                echo "Artifact'lar Jenkins'e yüklendi."
                script {
                    if (env.BRANCH_NAME == 'main' || env.BRANCH_NAME == 'master') {
                        echo "Modrinth'e yayınlandı!"
                    }
                }
                echo "Build zamanı: ${new Date()}"
            }
        }
    }

    // 6. Build Sonrası Aksiyonlar
    post {
        always {
            echo 'Build tamamlandı. Geçici dosyalar temizleniyor...'
            // Workspace'i tamamen temizlemek yerine sadece geçici dosyaları temizle
            sh '''
                find . -name "*.tmp" -delete 2>/dev/null || true
                find . -name "jdk.tar.gz" -delete 2>/dev/null || true
            '''
        }
        success {
            echo '✅ Build başarıyla tamamlandı!'
            echo "Commit ${env.GIT_COMMIT} başarıyla build edildi."
            // GitHub'a success status gönder (isteğe bağlı)
            script {
                try {
                    if (env.GIT_COMMIT && env.GIT_URL) {
                        // Git bilgilerini manuel olarak çıkar
                        def repoInfo = env.GIT_URL.replaceAll(/.*\/([^\/]+\/[^\/]+)\.git.*/, '$1')
                        githubNotify account: repoInfo.split('/')[0],
                                    repo: repoInfo.split('/')[1],
                                    sha: env.GIT_COMMIT,
                                    context: 'continuous-integration/jenkins',
                                    description: 'Build successful',
                                    status: 'SUCCESS'
                    }
                } catch (Exception e) {
                    echo "GitHub status gönderilemedi: ${e.getMessage()}"
                }
            }
        }
        failure {
            echo '❌ Build başarısız oldu!'
            echo "Hata detayları için build log'larını kontrol edin."
            // GitHub'a failure status gönder (isteğe bağlı)
            script {
                try {
                    if (env.GIT_COMMIT && env.GIT_URL) {
                        def repoInfo = env.GIT_URL.replaceAll(/.*\/([^\/]+\/[^\/]+)\.git.*/, '$1')
                        githubNotify account: repoInfo.split('/')[0],
                                    repo: repoInfo.split('/')[1],
                                    sha: env.GIT_COMMIT,
                                    context: 'continuous-integration/jenkins',
                                    description: 'Build failed',
                                    status: 'FAILURE'
                    }
                } catch (Exception e) {
                    echo "GitHub status gönderilemedi: ${e.getMessage()}"
                }
            }
        }
        changed {
            echo "Build durumu değişti: ${currentBuild.currentResult}"
        }
    }
}