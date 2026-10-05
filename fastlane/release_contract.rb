# Shared by lanes and executable contract tests; no publishing side effects.
module BreakoutRelease
  SIGNING_KEYS = %w[BP_RELEASE_STORE_FILE BP_RELEASE_STORE_PASSWORD BP_RELEASE_KEY_ALIAS BP_RELEASE_KEY_PASSWORD].freeze
  ROOT = File.expand_path("..", __dir__)

  def self.require_signing!
    missing = SIGNING_KEYS.select { |name| ENV[name].to_s.strip.empty? }
    raise "Release signing missing: #{missing.join(', ')}. Use compile_check for validation." unless missing.empty?
    raise "Release keystore does not exist" unless File.file?(ENV.fetch("BP_RELEASE_STORE_FILE"))
  end

  def self.require_play_key!
    key = ENV["GOOGLE_PLAY_JSON"] || ENV["PLAY_SERVICE_ACCOUNT_JSON"] || ENV["GOOGLE_PLAY_SERVICE_ACCOUNT_JSON"]
    raise "Play upload requires GOOGLE_PLAY_JSON pointing to a service-account file" if key.to_s.strip.empty? || !File.file?(key)
    key
  end

  def self.version_code
    properties = File.readlines(File.join(ROOT, "version.properties")).filter_map do |line|
      line.strip.split("=", 2) unless line.strip.empty? || line.start_with?("#")
    end.to_h
    Integer(properties.fetch("versionCode"), 10)
  end

  def self.aab_path
    File.join(ROOT, "app/build/outputs/bundle/release/app-release.aab")
  end
end
