pipeline {
    agent any
    
    environment {
        SERVER_PORT="1030"
        PROJECT = "spreadsheets-google"
        PROJECT_FOLDER = "data-collectors/${PROJECT}"
        ARTIFACT_NAME = "dc-${PROJECT}"
        GOOGLE_SECRET=credentials('spreadsheets.client_secret.json')
        GOOGLE_CREDENTIALS=credentials('google-spreadsheet-api-credentials')
        DOCKER_IMAGE = '755952719952.dkr.ecr.eu-west-1.amazonaws.com/spreadsheets-google-noiplaces'
        DOCKER_TAG = "prod-$BUILD_NUMBER"
        VENDOR = "umadummCarpooling"
    }

    stages {
        stage('Configure') {
            steps {
                sh """
                    cd ${PROJECT_FOLDER}
                    echo 'SERVER_PORT=${SERVER_PORT}' > .env
                    echo 'COMPOSE_PROJECT_NAME=${VENDOR}' >> .env
                    echo 'DOCKER_IMAGE=${DOCKER_IMAGE}' >> .env
                    echo 'DOCKER_TAG=${DOCKER_TAG}' >> .env
                    echo 'LOG_LEVEL=INFO' >> .env
                    echo 'ARTIFACT_NAME=${ARTIFACT_NAME}' >> .env
                    echo 'VENDOR=${VENDOR}' >> .env
                    echo 'spreadsheetId=1K49hBRbloKMtwxqBPHMf8yVFsu1xjFtUfk4CVtZhTMI' >> .env
                    echo 'suportedLanguages=en,de,it' >> .env
                    echo 'headers_nameId=it:name' >> .env
                    echo 'headers_longitudeId=longitude' >> .env
                    echo 'headers_latitudeId=latitude' >> .env
                    echo 'headers_metaDataId=metadata-id' >> .env
                    echo 'spreadsheet_range=A1:Z' >> .env
                    echo 'spreadsheet_notificationUrl=https://spreadsheets.opendatahub.bz.it/umadumm/trigger' >> .env
                    echo 'stationtype=CarpoolingHub' >> .env
                    echo 'composite_unique_key=id' >> .env
                    echo 'origin=ummadum' >> .env
                    echo -n 'provenance_version=' >> .env
                    xmlstarlet sel -N pom=http://maven.apache.org/POM/4.0.0 -t -v '/pom:project/pom:version' pom.xml >> .env
                    echo '' >> .env
                    echo -n 'provenance_name=' >> .env 
                    xmlstarlet sel -N pom=http://maven.apache.org/POM/4.0.0 -t -v '/pom:project/pom:artifactId' pom.xml >> .env
                    echo '' >> .env
                    echo 'authorizationUri=https://auth.opendatahub.bz.it/auth' >> .env
                    echo 'tokenUri=https://auth.opendatahub.bz.it/auth/realms/noi/protocol/openid-connect/token' >> .env 
                    echo 'clientId=odh-mobility-datacollector' >> .env
                    echo 'clientName=odh-mobility-datacollector' >> .env
                    echo 'clientSecret=${DATACOLLECTORS_CLIENT_SECRET}' >> .env
                    echo 'scope=openid' >> .env
                    echo 'BASE_URI=https://mobility.share.opendatahub.bz.it/json' >> .env
                """
                sh "cat ${GOOGLE_SECRET} > ${PROJECT_FOLDER}/src/main/resources/META-INF/spring/client_secret.json"
                sh """cat "${GOOGLE_CREDENTIALS}" > "${PROJECT_FOLDER}"/src/main/resources/META-INF/credentials/StoredCredential"""
            }
        }
        stage('Test & Build') {
            steps {
                sh """
                    cd ${PROJECT_FOLDER}
                    aws ecr get-login --region eu-west-1 --no-include-email | bash
                    docker-compose --no-ansi -f infrastructure/docker-compose.build.yml build --pull
                    docker-compose --no-ansi -f infrastructure/docker-compose.build.yml push
                """
            }
        }
        stage('Deploy') {
            steps {
               sshagent(['jenkins-ssh-key']) {
                    sh """
                        (cd ${PROJECT_FOLDER}/infrastructure/ansible && ansible-galaxy install -f -r requirements.yml)
                        (cd ${PROJECT_FOLDER}/infrastructure/ansible && ansible-playbook --limit=prod deploy.yml --extra-vars "release_name=${BUILD_NUMBER} project_name=${PROJECT}-${VENDOR}")
                    """
                }
            }
        }
    }
}
