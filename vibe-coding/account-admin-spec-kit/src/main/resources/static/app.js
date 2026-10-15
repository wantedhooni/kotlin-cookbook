"use strict";
// Credentials and account data remain in JS memory, not localStorage/sessionStorage.
let credentials = null;
let page = 0;
let totalPages = 0;
let toastTimer;

const $ = (id) => document.getElementById(id);
const loginForm = $("login-form");

function showMessage(message, isError = false) {
  const node = $("feedback");
  node.textContent = message;
  node.className = `feedback show${isError ? " error" : ""}`;
  clearTimeout(toastTimer);
  toastTimer = setTimeout(() => node.className = "feedback", 4500);
}

function authHeader(username, password) {
  const bytes = new TextEncoder().encode(`${username}:${password}`);
  let binary = "";
  for (const byte of bytes) binary += String.fromCharCode(byte);
  return `Basic ${btoa(binary)}`;
}

async function api(path, options = {}) {
  if (!credentials) throw new Error("관리자 로그인이 필요합니다.");
  const headers = {Authorization: credentials, ...options.headers};
  if (options.body) headers["Content-Type"] = "application/json";
  const res = await fetch(path, { ...options, headers, cache: "no-store" });
  if (!res.ok) {
    let message = `${res.status} ${res.statusText}`;
    try { const data = await res.json(); message = data.message || message; } catch (_) {}
    if (res.status === 401) message = "인증에 실패했습니다. ID/비밀번호를 확인하세요.";
    throw new Error(message);
  }
  return res.status === 204 ? null : res.json();
}

function td(text) { const cell = document.createElement("td"); cell.textContent = String(text ?? ""); return cell; }
function actionButton(text, callback, danger = false) {
  const b = document.createElement("button"); b.type = "button"; b.textContent = text;
  if (danger) b.classList.add("danger");
  b.addEventListener("click", async () => { try { b.disabled = true; await callback(); } catch (error) { showMessage(error.message, true); } finally { b.disabled = false; } });
  return b;
}

function makeRow(item) {
  const tr = document.createElement("tr");
  const user = document.createElement("td");
  const name = document.createElement("div"); name.className = "account-name"; name.textContent = item.displayName;
  const id = document.createElement("div"); id.className = "subtext"; id.textContent = item.loginId;
  user.append(name, id); tr.append(user, td(item.email));
  const role = document.createElement("td"); const roleBadge = document.createElement("span");
  roleBadge.className = "badge"; roleBadge.textContent = item.role; role.append(roleBadge); tr.append(role);
  const status = document.createElement("td"); const statusBadge = document.createElement("span");
  statusBadge.className = `badge ${item.status}`; statusBadge.textContent = item.status; status.append(statusBadge); tr.append(status);
  tr.append(td(new Date(item.createdAt).toLocaleDateString("ko-KR")));
  const actions = document.createElement("td"); const group = document.createElement("div"); group.className = "actions";
  if (item.status !== "DELETED") {
    group.append(actionButton("수정", async () => {
      const email = prompt("이메일", item.email); if (email === null) return;
      const displayName = prompt("표시 이름", item.displayName); if (displayName === null) return;
      await api(`/api/admin/accounts/${item.id}`, {method:"PUT", body: JSON.stringify({email, displayName})});
      showMessage("계정 정보가 수정되었습니다."); await loadAccounts();
    }));
    group.append(actionButton(item.role === "USER" ? "관리자 지정" : "일반 사용자", async () => {
      const next = item.role === "USER" ? "ADMIN" : "USER";
      if (!confirm(`역할을 ${next}(으)로 변경하시겠습니까?`)) return;
      await api(`/api/admin/accounts/${item.id}/role`, {method:"PATCH", body:JSON.stringify({role:next})});
      showMessage("역할이 변경되었습니다."); await loadAccounts();
    }));
    group.append(actionButton(item.status === "ACTIVE" ? "정지" : "활성화", async () => {
      const next = item.status === "ACTIVE" ? "SUSPENDED" : "ACTIVE";
      await api(`/api/admin/accounts/${item.id}/status`, {method:"PATCH", body:JSON.stringify({status:next})});
      showMessage("계정 상태가 변경되었습니다."); await loadAccounts();
    }));
    group.append(actionButton("삭제", async () => {
      if (!confirm("계정을 소프트 삭제하시겠습니까? 기존 로그인 ID와 이메일은 재사용할 수 없습니다.")) return;
      await api(`/api/admin/accounts/${item.id}`, {method:"DELETE"});
      showMessage("계정이 삭제되었습니다."); await loadAccounts();
    }, true));
  }
  actions.append(group); tr.append(actions); return tr;
}

async function loadAccounts() {
  const query = new URLSearchParams({page:String(page),size:"20"});
  const keyword = $("keyword").value.trim(); const role = $("role-filter").value; const status = $("status-filter").value;
  if (keyword) query.set("keyword", keyword);
  if (role) query.set("role", role);
  if (status) query.set("status", status);
  const result = await api(`/api/admin/accounts?${query}`);
  totalPages = result.totalPages;
  $("total-count").textContent = result.totalElements.toLocaleString("ko-KR");
  $("page-number").textContent = `${result.page + 1}`;
  $("page-info").textContent = `${result.page + 1} / ${Math.max(1, result.totalPages)}`;
  $("prev").disabled = page <= 0;
  $("next").disabled = page + 1 >= totalPages;
  const tbody = $("account-rows"); tbody.replaceChildren(...result.items.map(makeRow));
  if (result.items.length === 0) {
    const tr = document.createElement("tr"); const cell = td("조회된 계정이 없습니다."); cell.colSpan = 6; tr.append(cell); tbody.append(tr);
  }
}

loginForm.addEventListener("submit", async (event) => {
  event.preventDefault(); const data = new FormData(loginForm);
  credentials = authHeader(data.get("username"), data.get("password"));
  try {
    await loadAccounts(); $("login-section").hidden = true; $("dashboard").hidden = false;
    $("logout").hidden = false; loginForm.reset();
  } catch (error) { credentials = null; showMessage(error.message, true); }
});
$("logout").addEventListener("click", () => {
  credentials = null; $("dashboard").hidden = true; $("login-section").hidden = false;
  $("logout").hidden = true; $("account-rows").replaceChildren();
  // Browser HTTP Basic authentication cache (if used by the browser) can survive this UI logout.
});
$("filter-form").addEventListener("submit", async (event) => {
  event.preventDefault(); page = 0; try { await loadAccounts(); } catch (e) { showMessage(e.message, true); }
});
$("toggle-create").addEventListener("click", () => $("create-form").hidden = !$("create-form").hidden);
$("create-form").addEventListener("submit", async (event) => {
  event.preventDefault(); const data = Object.fromEntries(new FormData(event.target).entries());
  try { await api("/api/admin/accounts", {method:"POST",body:JSON.stringify(data)});
    event.target.reset(); event.target.hidden = true; page = 0; showMessage("계정이 생성되었습니다."); await loadAccounts();
  } catch (error) { showMessage(error.message, true); }
});
$("prev").addEventListener("click", async () => { if (page > 0) {page--; try { await loadAccounts(); } catch (e) { showMessage(e.message, true); }} });
$("next").addEventListener("click", async () => { if (page + 1 < totalPages) {page++; try { await loadAccounts(); } catch (e) { showMessage(e.message, true); }} });
