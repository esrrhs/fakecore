# Optional resources

`GeoLite2-Country.mmdb` is **not** packaged in the default fakecore jar (keeps the artifact small and license-clear).

To enable GeoIP lookups:

1. Copy `GeoLite2-Country.mmdb` onto your application's classpath (e.g. `src/main/resources/`), or
2. Pass it as a `-cp` entry at runtime.

`GeoIPUtil.getCountryCode(ip)` returns `null` when the database is absent.
