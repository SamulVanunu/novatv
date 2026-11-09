# NovaTV mock paneli

Geliştirme ve uçtan uca testler için üç kaynak tipini de taklit eden küçük bir sunucu.
Harici bağımlılık yoktur (Node 20+).

```bash
bash tools/mock-server/gen-hls.sh          # 60 sn HLS + TS fixture üretir
node tools/mock-server/server.mjs          # http://localhost:8089
adb reverse tcp:8089 tcp:8089              # emülatörden 127.0.0.1:8089
```

## Uçlar

| Kaynak | Adres / bilgiler |
|---|---|
| Xtream | `http://127.0.0.1:8089` — kullanıcı `demo`, şifre `demo` |
| M3U | `http://127.0.0.1:8089/get.php?username=demo&password=demo&type=m3u_plus` |
| Stalker | `http://127.0.0.1:8089/portal` — MAC `00:1A:79:00:00:01` |
| HLS | `http://127.0.0.1:8089/hls/index.m3u8` (60 sn, 6 sn segment) |
| TS | `http://127.0.0.1:8089/fixtures/test.ts` |

`/live/demo/demo/:id.m3u8` ve `:id.ts` adresleri fixture'lara 302 yönlendirir.
`PORT` ortam değişkeniyle port değiştirilebilir.
