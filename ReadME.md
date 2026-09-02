# URL Shortener — Design Notes

This document captures my current thinking on the URL Shortener design. It's a
working draft — the low-level design (LLD) diagram below is generated
automatically from the `.excalidraw` source via the GitHub Actions workflow in
this repo, so it always reflects the latest version of the diagram.

## LLD

![LLD](./LLD.svg)

## Flow

1. User gives a URL.
2. The URL is shortened and saved somewhere.
3. When someone visits the shortened URL, we redirect them to the original URL.

## Entities

- **USER**
- **ORIGINAL_URL**
- **SHORTEN_URL**

## URL_SHORTENER (current shape)

- `<String, String>` hashmap — maps shortened URL → original URL
- `func shortURL(originalURL) -> shortURL`
- `func sendOriginalURL(shortURL) -> originalURL`

## V1 — problems I see with this

1. The request interacts directly with the system — no separation between the
   API layer and the core shortening logic.
2. We should decouple the URL-shortening logic from storage. We need an actual
   storage layer instead of an in-memory hashmap.
3. Redirection is read-heavy — there will be many more reads (redirects) than
   writes (new short URLs), so we need to think about how we handle a high
   volume of concurrent redirect calls.
4. We need rate limiting, or we should queue incoming requests and serve them
   accordingly, instead of handling everything synchronously and directly.

## Open questions / next steps

- Decide on the storage layer (in-memory cache + persistent DB? which DB?).
- Design the short-code generation strategy (hash-based, counter-based,
  base62 encoding, collision handling).
- Figure out the caching strategy for read-heavy redirect traffic.
- Decide on rate limiting approach (per-user, per-IP, token bucket, queue-based).