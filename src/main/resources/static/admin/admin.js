const state = {
    token: "",
    email: "",
    users: [],
    backlog: [],
    activeTrips: [],
    nonOpenTrips: [],
    selectedUserIds: new Set(),
    fcmOnlyMode: false
};

const elements = {
    loginForm: document.getElementById("loginForm"),
    loginFormWrapper: document.getElementById("loginFormWrapper"),
    loginEmail: document.getElementById("loginEmail"),
    loginPassword: document.getElementById("loginPassword"),
    logoutBtn: document.getElementById("logoutBtn"),
    loggedInBanner: document.getElementById("loggedInBanner"),
    loggedInEmail: document.getElementById("loggedInEmail"),
    statusBanner: document.getElementById("statusBanner"),
    refreshAllBtn: document.getElementById("refreshAllBtn"),
    totalUsersMetric: document.getElementById("totalUsersMetric"),
    fcmUsersMetric: document.getElementById("fcmUsersMetric"),
    backlogMetric: document.getElementById("backlogMetric"),
    activeTripsMetric: document.getElementById("activeTripsMetric"),
    nonOpenTripsMetric: document.getElementById("nonOpenTripsMetric"),
    selectedUsersMetric: document.getElementById("selectedUsersMetric"),
    notificationForm: document.getElementById("notificationForm"),
    audienceSelect: document.getElementById("audienceSelect"),
    selectionSummary: document.getElementById("selectionSummary"),
    usersTableBody: document.getElementById("usersTableBody"),
    userSearchInput: document.getElementById("userSearchInput"),
    toggleFcmUsersBtn: document.getElementById("toggleFcmUsersBtn"),
    selectVisibleUsersBtn: document.getElementById("selectVisibleUsersBtn"),
    clearSelectionBtn: document.getElementById("clearSelectionBtn"),
    backlogTableBody: document.getElementById("backlogTableBody"),
    includeMatchedBacklogToggle: document.getElementById("includeMatchedBacklogToggle"),
    activeTripsList: document.getElementById("activeTripsList"),
    nonOpenTripsList: document.getElementById("nonOpenTripsList")
};

document.addEventListener("DOMContentLoaded", () => {
    initializeSession();
    bindEvents();
    renderUsers();
    renderBacklog();
    renderTrips();
});

function bindEvents() {
    elements.loginForm.addEventListener("submit", handleLoginSubmit);

    elements.logoutBtn.addEventListener("click", () => {
        state.token = "";
        state.email = "";
        localStorage.removeItem("swoopd.admin.token");
        localStorage.removeItem("swoopd.admin.email");
        elements.loginFormWrapper.style.display = "";
        elements.loggedInBanner.style.display = "none";
        elements.logoutBtn.style.display = "none";
        elements.loginEmail.value = "";
        elements.loginPassword.value = "";
        setStatus("Signed out successfully.", "info");
    });

    elements.refreshAllBtn.addEventListener("click", loadAll);
    elements.userSearchInput.addEventListener("input", renderUsers);
    elements.toggleFcmUsersBtn.addEventListener("click", () => {
        state.fcmOnlyMode = !state.fcmOnlyMode;
        elements.toggleFcmUsersBtn.textContent = state.fcmOnlyMode ? "Show all users" : "Show FCM-ready only";
        renderUsers();
    });
    elements.selectVisibleUsersBtn.addEventListener("click", selectVisibleUsers);
    elements.clearSelectionBtn.addEventListener("click", () => {
        state.selectedUserIds.clear();
        updateSelectionSummary();
        renderUsers();
    });
    elements.includeMatchedBacklogToggle.addEventListener("change", loadBacklog);
    elements.audienceSelect.addEventListener("change", updateSelectionSummary);
    elements.notificationForm.addEventListener("submit", handleNotificationSubmit);
}

function initializeSession() {
    const storedToken = localStorage.getItem("swoopd.admin.token") || "";
    const storedEmail = localStorage.getItem("swoopd.admin.email") || "";
    if (storedToken) {
        state.token = storedToken;
        state.email = storedEmail;
        showLoggedIn(storedEmail);
        setStatus("Session restored. Loading admin data...", "info");
        loadAll();
    }
}

function showLoggedIn(email) {
    elements.loginFormWrapper.style.display = "none";
    elements.loggedInBanner.style.display = "";
    elements.logoutBtn.style.display = "";
    elements.loggedInEmail.textContent = email || "admin";
}

async function handleLoginSubmit(event) {
    event.preventDefault();
    const email = elements.loginEmail.value.trim();
    const password = elements.loginPassword.value;

    try {
        setStatus("Signing in...", "info");
        const response = await fetch("/admin/login", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ email, password })
        });
        const payload = await response.json().catch(() => ({ success: false, message: "Unexpected server response" }));

        if (!response.ok || !payload.success) {
            setStatus(payload.message || "Login failed.", "error");
            return;
        }

        const rawToken = payload.data;
        state.token = rawToken.toLowerCase().startsWith("bearer ") ? rawToken : `Bearer ${rawToken}`;
        state.email = email;
        localStorage.setItem("swoopd.admin.token", state.token);
        localStorage.setItem("swoopd.admin.email", email);
        showLoggedIn(email);
        elements.loginPassword.value = "";
        setStatus("Signed in. Loading admin data...", "success");
        await loadAll();
    } catch (error) {
        setStatus(error.message || "Login request failed.", "error");
    }
}

async function loadAll() {
    if (!ensureToken()) {
        return;
    }

    try {
        setStatus("Loading admin data...", "info");
        await Promise.all([loadUsers(), loadBacklog(), loadActiveTrips(), loadNonOpenTrips()]);
        refreshMetrics();
        setStatus("Admin data loaded successfully.", "success");
    } catch (error) {
        handleError(error);
    }
}

async function loadUsers() {
    const response = await apiFetch("/admin/users");
    state.users = response.data || [];
    trimSelectionsToKnownUsers();
    renderUsers();
    refreshMetrics();
}

async function loadBacklog() {
    const includeMatched = elements.includeMatchedBacklogToggle.checked;
    const response = await apiFetch(`/admin/backlog?includeMatched=${includeMatched}`);
    state.backlog = response.data || [];
    renderBacklog();
    refreshMetrics();
}

async function loadActiveTrips() {
    const response = await apiFetch("/admin/trips/active");
    state.activeTrips = response.data || [];
    renderTrips();
    refreshMetrics();
}

async function loadNonOpenTrips() {
    const response = await apiFetch("/admin/trips/non-open");
    state.nonOpenTrips = response.data || [];
    renderTrips();
    refreshMetrics();
}

function renderUsers() {
    const searchValue = elements.userSearchInput.value.trim().toLowerCase();
    const filteredUsers = getVisibleUsers(searchValue);

    if (!filteredUsers.length) {
        elements.usersTableBody.innerHTML = `<tr><td colspan="6"><div class="empty-state">No users match the current filter.</div></td></tr>`;
        return;
    }

    elements.usersTableBody.innerHTML = filteredUsers.map((user) => {
        const selected = state.selectedUserIds.has(user.userId) ? "checked" : "";
        const roleClass = user.role === "ADMIN" ? "admin" : "user";
        const tokenClass = user.hasMessagingToken ? "ready" : "missing";
        return `
            <tr>
                <td><input type="checkbox" data-user-select="${user.userId}" ${selected}></td>
                <td>${escapeHtml(user.fullName || "Unknown user")}</td>
                <td>${escapeHtml(user.email || "")}</td>
                <td><span class="role-badge ${roleClass}">${escapeHtml(user.role || "NORMAL_USER")}</span></td>
                <td><span class="token-badge ${tokenClass}">${user.hasMessagingToken ? "Ready" : "Missing"}</span></td>
                <td>
                    <div class="user-actions">
                        <button class="button button-secondary" type="button" data-promote-user="${user.userId}">Promote</button>
                        <button class="button button-danger" type="button" data-demote-user="${user.userId}">Demote</button>
                    </div>
                </td>
            </tr>
        `;
    }).join("");

    elements.usersTableBody.querySelectorAll("[data-user-select]").forEach((checkbox) => {
        checkbox.addEventListener("change", (event) => {
            const { userSelect } = event.target.dataset;
            if (event.target.checked) {
                state.selectedUserIds.add(userSelect);
            } else {
                state.selectedUserIds.delete(userSelect);
            }
            updateSelectionSummary();
        });
    });

    elements.usersTableBody.querySelectorAll("[data-promote-user]").forEach((button) => {
        button.addEventListener("click", () => updateUserRole(button.dataset.promoteUser, "promote"));
    });

    elements.usersTableBody.querySelectorAll("[data-demote-user]").forEach((button) => {
        button.addEventListener("click", () => updateUserRole(button.dataset.demoteUser, "demote"));
    });
}

function renderBacklog() {
    if (!state.backlog.length) {
        elements.backlogTableBody.innerHTML = `<tr><td colspan="6"><div class="empty-state">No backlog entries to display.</div></td></tr>`;
        return;
    }

    elements.backlogTableBody.innerHTML = state.backlog.map((entry) => `
        <tr>
            <td>
                <strong>${escapeHtml(entry.fullName || "Unknown user")}</strong><br>
                <span class="muted">${escapeHtml(entry.email || "")}</span>
            </td>
            <td>${escapeHtml(entry.originZone || "-")}</td>
            <td>${escapeHtml(entry.destinationZone || "-")}</td>
            <td>${formatDateTime(entry.requestMadeAt)}</td>
            <td>${formatDateTime(entry.selectedDepartureTime)}</td>
            <td><span class="status-badge ${entry.matched ? "other" : "open"}">${entry.matched ? "Matched" : "Pending"}</span></td>
        </tr>
    `).join("");
}

function renderTrips() {
    renderTripCollection(elements.activeTripsList, state.activeTrips, "No active trips right now.");
    renderTripCollection(elements.nonOpenTripsList, state.nonOpenTrips, "No non-open trips available.");
}

function renderTripCollection(container, trips, emptyMessage) {
    if (!trips.length) {
        container.innerHTML = `<div class="empty-state">${escapeHtml(emptyMessage)}</div>`;
        return;
    }

    container.innerHTML = trips.map((trip) => {
        const statusClass = trip.tripStatus === "OPEN"
            ? "open"
            : trip.tripStatus === "FULL"
                ? "full"
                : "other";
        return `
            <article class="trip-card">
                <div class="panel-header">
                    <div>
                        <h4>Trip ${escapeHtml(shortId(trip.tripId))}</h4>
                        <p class="muted">Host: ${escapeHtml(trip.hostName || "Unknown")}${trip.hostEmail ? ` · ${escapeHtml(trip.hostEmail)}` : ""}</p>
                    </div>
                    <span class="status-badge ${statusClass}">${escapeHtml(trip.tripStatus || "UNKNOWN")}</span>
                </div>
                <div class="trip-grid">
                    <div><strong>Departure</strong><br>${formatDateTime(trip.departureTime)}</div>
                    <div><strong>Route</strong><br>${escapeHtml(trip.originZone || "-")} → ${escapeHtml(trip.destinationZone || "-")}</div>
                    <div><strong>Vehicle</strong><br>${escapeHtml(trip.vehicleRegNumber || "-")}</div>
                    <div><strong>Remaining capacity</strong><br>${trip.remainingCapacity ?? 0}</div>
                    <div><strong>Participants</strong><br>${trip.participantCount ?? 0}</div>
                    <div><strong>Members</strong><br>${escapeHtml((trip.participantNames || []).join(", ") || "-")}</div>
                </div>
            </article>
        `;
    }).join("");
}

async function updateUserRole(userId, action) {
    if (!ensureToken()) {
        return;
    }

    const actionLabel = action === "promote" ? "promote" : "demote";
    try {
        const response = await apiFetch(`/admin/users/${userId}/${actionLabel}`, { method: "POST" });
        setStatus(response.message || `User ${actionLabel}d successfully.`, "success");
        await loadUsers();
    } catch (error) {
        handleError(error);
    }
}

async function handleNotificationSubmit(event) {
    event.preventDefault();
    if (!ensureToken()) {
        return;
    }

    const selectedChannels = Array.from(document.querySelectorAll('input[name="channels"]:checked'))
        .map((input) => input.value);

    const payload = {
        title: document.getElementById("notificationTitle").value.trim(),
        message: document.getElementById("notificationMessage").value.trim(),
        audience: elements.audienceSelect.value,
        channels: selectedChannels
    };

    if (payload.audience === "SELECTED_USERS") {
        payload.selectedUserIds = Array.from(state.selectedUserIds);
    }

    try {
        const response = await apiFetch("/admin/notifications", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(payload)
        });
        const result = response.data || {};
        setStatus(
            `Announcement sent. Targeted ${result.targetedUsers || 0} users, ${result.fcmRecipients || 0} via Firebase, ${result.emailRecipients || 0} via email.`,
            "success"
        );
        elements.notificationForm.reset();
        elements.audienceSelect.value = "ALL_USERS";
        updateSelectionSummary();
    } catch (error) {
        handleError(error);
    }
}

function getVisibleUsers(searchValue) {
    return state.users.filter((user) => {
        if (state.fcmOnlyMode && !user.hasMessagingToken) {
            return false;
        }
        if (!searchValue) {
            return true;
        }
        const haystack = `${user.fullName || ""} ${user.email || ""}`.toLowerCase();
        return haystack.includes(searchValue);
    });
}

function selectVisibleUsers() {
    const visibleUsers = getVisibleUsers(elements.userSearchInput.value.trim().toLowerCase());
    visibleUsers.forEach((user) => state.selectedUserIds.add(user.userId));
    updateSelectionSummary();
    renderUsers();
}

function trimSelectionsToKnownUsers() {
    const knownIds = new Set(state.users.map((user) => user.userId));
    Array.from(state.selectedUserIds).forEach((userId) => {
        if (!knownIds.has(userId)) {
            state.selectedUserIds.delete(userId);
        }
    });
    updateSelectionSummary();
}

function refreshMetrics() {
    elements.totalUsersMetric.textContent = state.users.length;
    elements.fcmUsersMetric.textContent = state.users.filter((user) => user.hasMessagingToken).length;
    elements.backlogMetric.textContent = state.backlog.length;
    elements.activeTripsMetric.textContent = state.activeTrips.length;
    elements.nonOpenTripsMetric.textContent = state.nonOpenTrips.length;
    elements.selectedUsersMetric.textContent = state.selectedUserIds.size;
}

function updateSelectionSummary() {
    const selectedCount = state.selectedUserIds.size;
    elements.selectedUsersMetric.textContent = selectedCount;
    elements.selectionSummary.textContent = elements.audienceSelect.value === "SELECTED_USERS"
        ? `${selectedCount} user(s) selected for the next announcement.`
        : "Currently targeting all users.";
}

async function apiFetch(url, options = {}) {
    const headers = new Headers(options.headers || {});
    headers.set("Authorization", state.token);

    const response = await fetch(url, { ...options, headers });
    const payload = await response.json().catch(() => ({ success: false, message: "Unexpected server response" }));

    if (!response.ok || payload.success === false) {
        throw new Error(payload.message || `Request failed with status ${response.status}`);
    }

    return payload;
}

function ensureToken() {
    if (!state.token) {
        setStatus("Please sign in before calling admin endpoints.", "error");
        return false;
    }
    return true;
}

function normalizeToken(value) {
    const trimmed = (value || "").trim();
    if (!trimmed) {
        return "";
    }
    return trimmed.toLowerCase().startsWith("bearer ") ? trimmed : `Bearer ${trimmed}`;
}

function handleError(error) {
    setStatus(error.message || "Something went wrong.", "error");
}

function setStatus(message, variant) {
    elements.statusBanner.textContent = message;
    elements.statusBanner.className = `status-banner ${variant}`;
}

function formatDateTime(value) {
    if (!value) {
        return "-";
    }
    const parsed = new Date(value);
    if (Number.isNaN(parsed.getTime())) {
        return value;
    }
    return parsed.toLocaleString();
}

function shortId(value) {
    return value ? value.slice(0, 8) : "unknown";
}

function escapeHtml(value) {
    return String(value)
        .replaceAll("&", "&amp;")
        .replaceAll("<", "&lt;")
        .replaceAll(">", "&gt;")
        .replaceAll('"', "&quot;")
        .replaceAll("'", "&#39;");
}

