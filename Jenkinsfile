// Jenkins Pipeline Script for ATOMLAND Optimizer
// GitHub webhook ile otomatik build tetikleme
pipeline {
    // 1. Agent Ayarı
    agent any

    // 2. Ortam Değişkenleri
    environment {
        MAVEN_TOOL_NAME = 'Maven 3.9.6'
        JDK_URL = "https://github.com/adoptium/temurin21-binaries/releases/download/jdk-21.0.4%2B7/OpenJDK21U-jdk_x64_linux_hotspot_21.0.4_7.tar.gz"
        JDK_DIR_NAME = 'jdk-21'
    }

    // 3. Tetikleyiciler devre dışı - Manuel build için
    // triggers {
    //     pollSCM('* * * * *')
    // }

    // 4. Pipeline Seçenekleri
    options {
        // Build geçmişini sınırla
        buildDiscarder(logRotator(numToKeepStr: '10'))
        // Eş zamanlı build'leri önle
        disableConcurrentBuilds()
        // Zaman aşımı ayarla
        timeout(time: 30, unit: 'MINUTES')
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
                    echo "Build tetikleyen commit: ${commitId}"
                    echo "Commit mesajı: ${commitMessage}"
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

        // Aşama 4: Build Bildirim
        stage('Notification') {
            steps {
                echo "Build başarıyla tamamlandı!"
                echo "Artifact'lar Jenkins'e yüklendi."
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
        }
        failure {
            echo '❌ Build başarısız oldu!'
            echo "Hata detayları için build log'larını kontrol edin."
        }
        changed {
            echo "Build durumu değişti: ${currentBuild.currentResult}"
        }
    }
}