require_relative "../fastlane/release_contract"
raise "Invalid version" unless BreakoutRelease.version_code.positive?
keys = BreakoutRelease::SIGNING_KEYS + %w[GOOGLE_PLAY_JSON PLAY_SERVICE_ACCOUNT_JSON GOOGLE_PLAY_SERVICE_ACCOUNT_JSON CI]
saved = keys.to_h { |key| [key, ENV[key]] }
begin
  keys.each { |key| ENV.delete(key) }
  ENV["CI"] = "true"
  %i[require_signing! require_play_key!].each do |method|
    rejected = false
    begin
      BreakoutRelease.public_send(method)
    rescue RuntimeError
      rejected = true
    end
    raise "#{method} accepted missing credentials" unless rejected
  end
  puts "Release credential contract passed (CI does not bypass signing)"
ensure
  saved.each { |key, value| value ? ENV[key] = value : ENV.delete(key) }
end
