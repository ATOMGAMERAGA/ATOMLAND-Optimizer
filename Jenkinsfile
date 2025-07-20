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

                    withEnv(["JAVA_HOME=${jdkHome}", "PATH+MAVEN=${mavenHome}/bin:${jdkHome}/bin"]) {
                        echo "Kullanılan Java sürümü kontrol ediliyor..."
                        sh 'java -version'
                        echo "Kullanılan Maven sürümü kontrol ediliyor..."
                        sh 'mvn -v'
                        echo "Proje Maven ile derleniyor... Build Numarası: ${env.BUILD_NUMBER}"
                        echo "Git Branch: ${env.GIT_BRANCH}"
                        sh 'mvn clean package -DskipTests=false'

                        // JAR dosyasının tam yolunu bul ve global değişkene kaydet
                        def jarFile = sh(returnStdout: true, script: 'find target -name "*.jar" -type f | head -1').trim()
                        if (jarFile) {
                            env.JAR_FILE_PATH = jarFile
                            echo "JAR dosyası bulundu: ${jarFile}"
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
                anyOf {
                    branch 'main'
                    branch 'master'
                    // Veya tag push'larında yayınlamak istiyorsanız:
                    // tag pattern: 'v*', comparator: 'REGEXP'
                }
            }
            steps {
                echo 'Modrinth\'e yayınlanıyor...'
                withCredentials([string(credentialsId: 'MODRINTH_TOKEN', variable: 'MODRINTH_API_TOKEN')]) {
                    script {
                        // pom.xml'den versiyonu oku
                        def pomVersion = sh(returnStdout: true, script: 'mvn help:evaluate -Dexpression=project.version -q -DforceStdout').trim()
                        def versionNumber = "${pomVersion}"
                        def fileName = sh(returnStdout: true, script: 'basename ${JAR_FILE_PATH}').trim()

                        echo "Yayınlanacak versiyon: ${versionNumber}"
                        echo "Dosya adı: ${fileName}"

                        // Changelog'u commit mesajından oluştur
                        def changelog = env.COMMIT_MESSAGE ?: "Yeni sürüm: ${versionNumber}"

                        // Modrinth API'ye yayınla
                        sh """
                            curl -X POST 'https://api.modrinth.com/v2/version' \\
                                -H 'Authorization: ${MODRINTH_API_TOKEN}' \\
                                -H 'Content-Type: multipart/form-data' \\
                                -F 'data={
                                    "name": "${versionNumber}",
                                    "version_number": "${versionNumber}",
                                    "changelog": "${changelog}",
                                    "dependencies": [],
                                    "game_versions": ${MINECRAFT_VERSIONS},
                                    "version_type": "release",
                                    "loaders": ${LOADERS},
                                    "featured": false,
                                    "project_id": "${MODRINTH_PROJECT_ID}"
                                }' \\
                                -F 'file=@${JAR_FILE_PATH};filename=${fileName}'
                        """

                        echo "✅ Modrinth'e başarıyla yayınlandı!"
                    }
                }
            }
        }

        // Aşama 5: Build Bildirim
        stage('Notification') {
            steps {
                echo "Build başarıyla tamamlandı!"
                echo "Artifact'lar Jenkins'e yüklendi."
                when {
                    anyOf {
                        branch 'main'
                        branch 'master'
                    }
                }
                echo "Modrinth'e yayınlandı!"
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
                    githubNotify context: 'continuous-integration/jenkins',
                              description: 'Build successful',
                              status: 'SUCCESS'
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
                    githubNotify context: 'continuous-integration/jenkins',
                              description: 'Build failed',
                              status: 'FAILURE'
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