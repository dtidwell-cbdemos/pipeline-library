// vars/kubeDeploy.groovy
def call(imageName, imageTag) {
    def label = "kubectl"
    def podYaml = libraryResource 'podtemplates/kubeDeploy.yml'
    def deployYaml = libraryResource 'k8s/basicDeploy.yml'
    def repoName = env.IMAGE_REPO.toLowerCase()
    def envProdRepo = "environment_prod"
    
    podTemplate(name: 'kubectl', label: label, yaml: podYaml) {
      node(label) {
          //create environment repo for prod if it doesn't already exist
          withCredentials([string(credentialsId: "${githubCredentialId}", passwordVariable: 'ACCESS_TOKEN')]) {
            sh """curl -H "Authorization: token ${ACCESS_TOKEN}" https://api.github.com/repos/${repoOwner}/${envProdRepo} """
            //curl -H "Authorization: token ACCESS_TOKEN" --data '{"name":""}' https://api.github.com/orgs/ORGANISATION_NAME/repos
          }
        writeFile file: "deploy.yml", text: deployYaml
        container("kubectl") {
          sh("sed -i.bak 's#REPLACE_IMAGE_TAG#gcr.io/core-workshop/${repoName}:${BUILD_NUMBER}#' deploy.yml")
          sh("sed -i.bak 's#REPLACE_SERVICE_NAME#${repoName}#' deploy.yml")
          sh "kubectl apply -f deploy.yml"
          sh "echo 'deployed to http://prod.cb-sa.io/${repoName}/'"
        }
      }
    }
}
