#!/usr/bin/env python3
import json
import os
import sys
import time
from urllib import request, parse, error


def build_base_url() -> str:
    port = os.getenv("ARCHI_MCP_PORT", "8765")
    host = os.getenv("ARCHI_MCP_HOST", "127.0.0.1")
    return f"http://{host}:{port}"


def http_request(method: str, base: str, path: str, params: dict | None = None, json_body: dict | list | None = None, timeout: float = 60.0) -> tuple[int, object | None]:
    url = f"{base}{path}"
    if params:
        query = parse.urlencode([(k, v) for k, vv in params.items() for v in (vv if isinstance(vv, (list, tuple)) else [vv])])
        url = f"{url}?{query}"
    data_bytes = None
    headers = {"Accept": "application/json"}
    if json_body is not None:
        headers["Content-Type"] = "application/json"
        data_bytes = json.dumps(json_body).encode("utf-8")
    req = request.Request(url, data=data_bytes, method=method, headers=headers)
    try:
        with request.urlopen(req, timeout=timeout) as resp:
            status = resp.getcode()
            content_type = resp.headers.get("Content-Type", "")
            if "application/json" in content_type:
                body = json.loads(resp.read().decode("utf-8"))
            else:
                raw = resp.read()
                body = {"bytes": len(raw)} if raw is not None else None
            return status, body
    except error.HTTPError as e:
        try:
            body = json.loads(e.read().decode("utf-8"))
        except Exception:
            body = {"error": str(e)}
        return e.code, body
    except Exception as e:
        return 0, {"error": str(e)}


def pretty(obj: object | None) -> str:
    return json.dumps(obj, ensure_ascii=False, indent=2)


def run_step(desc: str, func):
    print(f"\n[SMOKE] {desc}")
    status, body = func()
    print(pretty({"status": status, "body": body}))
    return status, body


def wait_ready(base: str, attempts: int = 40, delay: float = 0.5):
    for _ in range(attempts):
        status, _ = http_request("GET", base, "/status", timeout=5.0)
        if status == 200:
            return True
        time.sleep(delay)
    print(f"[SMOKE] Server not reachable at {base}/status", file=sys.stderr)
    return False


def main() -> int:
    base = build_base_url()
    print(f"[SMOKE] Base: {base}")

    if not wait_ready(base):
        return 1

    run_step("GET /status", lambda: http_request("GET", base, "/status"))
    run_step("GET /openapi.json", lambda: http_request("GET", base, "/openapi.json"))
    run_step("GET /types", lambda: http_request("GET", base, "/types"))
    script_status, script_body = run_step("GET /script/engines", lambda: http_request("GET", base, "/script/engines"))
    if script_status == 200 and isinstance(script_body, dict) and script_body.get("installed"):
        run_step(
            "POST /script/run (noop)",
            lambda: http_request("POST", base, "/script/run", json_body={"code": "return 1"})
        )

    code, _ = http_request("GET", base, "/folders")
    if code == 409:
        print("\n[SMOKE] No active model (HTTP 409). Model-dependent checks are skipped.")
        return 0
    run_step("GET /folders", lambda: http_request("GET", base, "/folders"))
    print("\n[SMOKE] Active model detected. Proceeding to batch operations...")

    create_elements_body = [
        {"type": "business-actor", "name": "E1"},
        {"type": "business-role", "name": "E2"},
        {"type": "business-process", "name": "E3"},
        {"type": "business-function", "name": "E4"},
        {"type": "business-event", "name": "E5"},
    ]

    print("\n[SMOKE] Starting element batch operations")
    st, body = http_request("POST", base, "/elements", json_body=create_elements_body)
    if st not in (200, 201):
        print("[SMOKE] Batch POST /elements not supported, falling back to per-item")
        created = []
        for item in create_elements_body:
            _, b = http_request("POST", base, "/elements", json_body=item)
            created.append(b)
        body = created
    elements = body if isinstance(body, list) else [body]
    print(pretty(elements))
    eids = [el.get("id") for el in elements]

    st, eg = http_request("GET", base, "/elements", params={"ids": eids})
    if st == 200:
        run_step("GET /elements?ids=...", lambda: (st, eg))
    else:
        print("\n[SMOKE] GET /elements?ids not supported, per-id fallback")
        per = []
        for eid in eids:
            _, b = http_request("GET", base, f"/elements/{eid}")
            per.append(b)
        print(pretty(per))

    patch_elements_body = [{"id": eids[0], "name": "E1r"}, {"id": eids[1], "name": "E2r"}, {"id": eids[2], "name": "E3r"}]
    st, _ = http_request("PATCH", base, "/elements")
    if st == 200:
        run_step("PATCH /elements", lambda: http_request("PATCH", base, "/elements", json_body=patch_elements_body))
    else:
        print("\n[SMOKE] PATCH /elements not supported, per-id fallback")
        for item in patch_elements_body:
            http_request("PATCH", base, f"/elements/{item['id']}", json_body={"name": item["name"]})

    print("\n[SMOKE] Starting relation batch operations")
    rel_create_body = [
        {"type": "association-relationship", "sourceId": eids[0], "targetId": eids[1]},
        {"type": "association-relationship", "sourceId": eids[1], "targetId": eids[2]},
        {"type": "association-relationship", "sourceId": eids[2], "targetId": eids[3]},
        {"type": "association-relationship", "sourceId": eids[3], "targetId": eids[4]},
    ]
    st, body = http_request("POST", base, "/relations", json_body=rel_create_body)
    if st not in (200, 201):
        print("[SMOKE] Batch POST /relations not supported, per-item fallback")
        created = []
        for item in rel_create_body:
            _, b = http_request("POST", base, "/relations", json_body=item)
            created.append(b)
        body = created
    relations = body if isinstance(body, list) else [body]
    print(pretty(relations))
    rids = [r.get("id") for r in relations]

    st, rg = http_request("GET", base, "/relations", params={"ids": rids})
    if st == 200:
        run_step("GET /relations?ids=...", lambda: (st, rg))
    else:
        print("\n[SMOKE] GET /relations?ids not supported, per-id fallback")
        per = []
        for rid in rids:
            _, b = http_request("GET", base, f"/relations/{rid}")
            per.append(b)
        print(pretty(per))

    patch_relations_body = [{"id": rids[0], "name": "R1r"}, {"id": rids[1], "name": "R2r"}]
    st, _ = http_request("PATCH", base, "/relations")
    if st == 200:
        run_step("PATCH /relations", lambda: http_request("PATCH", base, "/relations", json_body=patch_relations_body))
    else:
        print("\n[SMOKE] PATCH /relations not supported, per-id fallback")
        for item in patch_relations_body:
            http_request("PATCH", base, f"/relations/{item['id']}", json_body={"name": item["name"]})

    print("\n[SMOKE] Adding to an existing view (first available)")
    _, views_list = run_step("GET /views", lambda: http_request("GET", base, "/views"))
    if isinstance(views_list, list) and views_list:
        existing_vid = views_list[0].get("id")
    else:
        # Fallback: create a view if none exists
        _, v = run_step("POST /views (create default)", lambda: http_request("POST", base, "/views", json_body={"type": "ArchimateDiagramModel", "name": "Smoke Default"}))
        existing_vid = v.get("id") if isinstance(v, dict) else None
    if not existing_vid:
        print("[SMOKE] No view available to add to", file=sys.stderr)
        return 2

    add_elements_to_view_body = [
        {"elementId": eids[0], "bounds": {"x": 10, "y": 10, "w": 40, "h": 40}},
        {"elementId": eids[1]},
        {"elementId": eids[2]},
        {"elementId": eids[3]},
        {"elementId": eids[4]},
    ]
    st, _ = run_step(f"POST /views/{{id}}/add-element (existing {existing_vid})", lambda: http_request("POST", base, f"/views/{existing_vid}/add-element", json_body=add_elements_to_view_body))
    if st not in (200, 201):
        print("[SMOKE] Batch add-element not supported, per-item fallback")
        for item in add_elements_to_view_body:
            run_step(f"POST /views/{{id}}/add-element item (existing {existing_vid})", lambda item=item: http_request("POST", base, f"/views/{existing_vid}/add-element", json_body=item))
    add_relations_to_view_body = [{"relationId": rid} for rid in rids]
    st, _ = run_step(f"POST /views/{{id}}/add-relation (existing {existing_vid})", lambda: http_request("POST", base, f"/views/{existing_vid}/add-relation", json_body=add_relations_to_view_body))
    if st not in (200, 201):
        print("[SMOKE] Batch add-relation not supported, per-item fallback")
        for rid in rids:
            run_step(f"POST /views/{{id}}/add-relation item (existing {existing_vid})", lambda rid=rid: http_request("POST", base, f"/views/{existing_vid}/add-relation", json_body={"relationId": rid}))

    # Do not delete from existing view; leave visible in Archi

    print("\n[SMOKE] Create/Delete temp view and one element")
    _, tmp_view = run_step("POST /views (temp)", lambda: http_request("POST", base, "/views", json_body={"type": "ArchimateDiagramModel", "name": "Smoke Temp View"}))
    tmp_vid = tmp_view.get("id") if isinstance(tmp_view, dict) else None
    if not tmp_vid:
        print("[SMOKE] Failed to create temp view", file=sys.stderr)
        return 2
    # Create temp element and add to temp view, then remove and cleanup
    _, tmp_el = run_step("POST /elements (temp element)", lambda: http_request("POST", base, "/elements", json_body={"type": "business-actor", "name": "TempEl"}))
    tmp_eid = (tmp_el or {}).get("id") if isinstance(tmp_el, dict) else None
    if tmp_eid:
        run_step("POST /views/{id}/add-element (temp)", lambda: http_request("POST", base, f"/views/{tmp_vid}/add-element", json_body={"elementId": tmp_eid, "bounds": {"x": 30, "y": 30, "w": 60, "h": 40}}))
        # Test style support
        run_step("POST /views/{id}/add-element (temp with style)", lambda: http_request("POST", base, f"/views/{tmp_vid}/add-element", json_body={"elementId": tmp_eid, "bounds": {"x": 100, "y": 100, "w": 80, "h": 60}, "style": {"fillColor": "#4CAF50", "fontColor": "#FFFFFF", "fontSize": "14"}}))
        # Get content to find objectId, then delete the object from the temp view
        _, tv_content = run_step("GET /views/{id}/content (temp)", lambda: http_request("GET", base, f"/views/{tmp_vid}/content"))
        if isinstance(tv_content, dict):
            objs = tv_content.get("objects") or []
            for o in objs:
                oid = o.get("objectId")
                if oid:
                    run_step("DELETE /views/{id}/objects/{objectId} (temp)", lambda oid=oid: http_request("DELETE", base, f"/views/{tmp_vid}/objects/{oid}"))
                    break
        # Delete temp element from model
        run_step("DELETE /elements (temp element)", lambda: http_request("DELETE", base, "/elements", json_body=[{"id": tmp_eid}]))
    # Finally delete temp view
    run_step("DELETE /views (temp)", lambda: http_request("DELETE", base, f"/views/{tmp_vid}"))

    del_rels_body = [{"id": rid} for rid in rids]
    run_step("DELETE /relations", lambda: http_request("DELETE", base, "/relations", json_body=del_rels_body))
    del_elems_body = [{"id": eid} for eid in eids]
    run_step("DELETE /elements", lambda: http_request("DELETE", base, "/elements", json_body=del_elems_body))

    # Test script execution
    print("\n[SMOKE] Test script execution")
    run_step("GET /script/engines", lambda: http_request("GET", base, "/script/engines"))
    run_step("POST /script/run (simple test)", lambda: http_request("POST", base, "/script/run", json_body={"code": "console.log('Smoke test script executed successfully');", "engine": "ajs"}))
    
    run_step("POST /model/save", lambda: http_request("POST", base, "/model/save", json_body={}))
    print("\n[SMOKE] Flow completed.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())


