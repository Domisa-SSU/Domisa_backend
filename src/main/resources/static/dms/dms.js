document.addEventListener("submit", function (event) {
	const form = event.target;
	if (!(form instanceof HTMLFormElement)) {
		return;
	}

	const message = form.dataset.confirm;
	if (message && !window.confirm(message)) {
		event.preventDefault();
	}
});

document.addEventListener("change", function (event) {
	const field = event.target;
	if (!(field instanceof HTMLSelectElement)) {
		return;
	}

	const form = field.form;
	if (form && form.dataset.autoSubmit === "true") {
		form.requestSubmit();
	}
});

if (window.location.pathname.startsWith("/dms-room") && window.location.pathname !== "/dms-room/login") {
	const keepAlive = function () {
		fetch("/dms-room/session/ping", { method: "GET", credentials: "same-origin", cache: "no-store" })
			.then(function (response) {
				if (response.redirected) {
					window.location.href = "/dms-room/login";
				}
			})
			.catch(function () {
			});
	};

	setInterval(keepAlive, 4 * 60 * 1000);
}
