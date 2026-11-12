<!-- generated:start -->
# GET /uploads/**

- **Accepts:** -
- **Returns:** `file bytes (static resource)`
- **Status codes:** 200, 404

## Flow

Follow the link to the first step; each step links to the next.

- [[WebConfig.addResourceHandlers]] _(config)_
  - **filesystem READ** `serve /uploads/**` filesystem:upload.dir (file)
<!-- generated:end -->

## Notes

