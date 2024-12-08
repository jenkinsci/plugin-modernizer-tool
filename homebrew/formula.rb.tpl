class PluginModernizer < Formula
    desc "Plugin Modernizer"
    version "{{projectVersion}}".split(".")[0]
    homepage "https://github.com/jenkins-infra/plugin-modernizer-tool"
    url "https://repo.jenkins-ci.org/artifactory/releases/io/jenkins/plugin-modernizer/{{distributionName}}-cli/{{projectVersion}}/{{distributionName}}-cli-{{projectVersion}}.jar"
    sha256 "{{distributionChecksumSha256}}"
    license "MIT"

    def install
      libexec.install "{{distributionName}}-cli-{{projectVersion}}.jar"
      bin.write_jar_script libexec/"{{distributionName}}-cli-{{projectVersion}}.jar", "plugin-modernizer"
    end

    test do
      system bin/"plugin-modernizer", "--version"
    end
  end
