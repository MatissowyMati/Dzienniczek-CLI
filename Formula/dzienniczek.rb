class Dzienniczek < Formula
  desc "CLI do VULCAN, eduVULCAN i Librus, przyjazne agentom AI"
  homepage "https://github.com/MatissowyMati/Dzienniczek-CLI"
  url "https://github.com/MatissowyMati/Dzienniczek-CLI.git", branch: "main"
  version "1.1.0"
  license "MIT"

  depends_on "openjdk@17"

  def install
    ENV["JAVA_HOME"] = Formula["openjdk@17"].opt_prefix
    system "./gradlew", ":cli:installDist", "--no-daemon"
    libexec.install Dir["cli/build/install/dzienniczek/*"]
    bin.write_env_script libexec/"bin/dzienniczek", JAVA_HOME: Formula["openjdk@17"].opt_prefix
    bash_completion.install "completions/dzienniczek.bash" => "dzienniczek"
    zsh_completion.install "completions/_dzienniczek"
    fish_completion.install "completions/dzienniczek.fish"
  end

  test do
    assert_match "1.1.0", shell_output("#{bin}/dzienniczek version --format table")
  end
end
