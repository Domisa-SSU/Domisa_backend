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
