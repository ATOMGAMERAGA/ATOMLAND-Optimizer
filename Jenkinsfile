// Jenkins Pipeline Script for ATOMLAND Optimizer
// Bu script, Java kurulumunu ve derlemeyi tek bir yerden, hatasız bir şekilde yönetir.

pipeline {
    // 1. Agent Ayarı
    agent any

    // 2. Ortam Değişkenleri
    // İhtiyacımız olan her şeyi burada tanımlıyoruz.
    environment {
        // Maven kurulumunun "Global Tool Configuration"daki adını belirtiyoruz.
        MAVEN_TOOL_NAME = 'Maven 3.9.6'
        // İndireceğimiz JDK'nın linki.
        JDK_URL = "https://github.com/adoptium/temurin21-binaries/releases/download/jdk-21.0.4%2B7/OpenJDK21U-jdk_x64_linux_hotspot_21.0.4_7.tar.gz"
        // JDK'yı açacağımız klasörün adı.
        JDK_DIR_NAME = 'jdk-21'
    }

    // 3. Tetikleyiciler (Triggers)
    // Bu blok, GitHub'dan gelen push sinyallerini dinler.
    triggers {
        githubPush()
    }

    // 4. Aşamalar (Stages)
    stages {
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
                        wget --quiet -O jdk.tar.gz "${JDK_URL}"
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
                        sh 'mvn clean package'
                    }
                }
            }
        }

        // Aşama 3: Oluşturulan Dosyayı Arşivle
        stage('Archive Artifact') {
            steps {
                echo 'Oluşturulan .jar dosyası arşivleniyor...'
                archiveArtifacts artifacts: 'target/*.jar', fingerprint: true
            }
        }
    }

    // 5. Build Sonrası Aksiyonlar
    post {
        always {
            echo 'Build tamamlandı. Çalışma alanı (workspace) temizleniyor...'
            cleanWs()
        }
        success {
            echo 'Build başarıyla tamamlandı!'
        }
        failure {
            echo 'Build başarısız oldu!'
        }
    }
}
