class PluginModernizer < Formula
    desc "Plugin Modernizer"
    # Note: Brew don't really like our versions scheme for CD. Implicitly it consider 499.vb_86f97f0b_197 as version 197 which is incorrect
    # So using version which  only first numeric part for CD
    version "{{projectVersion}}".split(".")[0]
    homepage "https://github.com/jenkins-infra/plugin-modernizer-tool"
    url "https://github.com/jenkins-infra/plugin-modernizer-tool/releases/download/{{projectVersion}}/jenkins-plugin-modernizer-{{projectVersion}}.jar"
    sha256 "{{distributionChecksumSha256}}"
    license "MIT"

    def install
      libexec.install "jenkins-plugin-modernizer-{{projectVersion}}.jar"
      bin.write_jar_script libexec/"jenkins-plugin-modernizer-{{projectVersion}}.jar", "plugin-modernizer"
    end

    test do
      system bin/"plugin-modernizer", "--version"
    end
  end
