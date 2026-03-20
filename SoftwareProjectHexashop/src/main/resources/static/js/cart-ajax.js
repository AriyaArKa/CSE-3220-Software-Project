(function () {
  var notice = document.getElementById("cartNotice");

  function updateCartBadges(cartItemCount) {
    if (typeof cartItemCount === "undefined" || cartItemCount === null) {
      return;
    }

    var selectors = [
      "[data-cart-count]",
      "a[href$='/cart'] span",
      "a[href='/cart'] span",
      "a[href*='/cart'] span",
      "i.fa-shopping-cart + span",
    ];

    var seen = new Set();
    selectors.forEach(function (selector) {
      document.querySelectorAll(selector).forEach(function (el) {
        if (!seen.has(el)) {
          el.textContent = String(cartItemCount);
          seen.add(el);
        }
      });
    });
  }

  function showNotice(message, isError) {
    if (!notice) {
      return;
    }
    var textEl = notice.querySelector("[data-cart-notice-text]");
    if (textEl) {
      textEl.textContent = message;
    }
    if (isError) {
      notice.classList.add("error");
    } else {
      notice.classList.remove("error");
    }
    notice.classList.add("show");
  }

  function hideNotice() {
    if (!notice) {
      return;
    }
    notice.classList.remove("show");
  }

  if (notice) {
    var closeBtn = notice.querySelector("[data-cart-notice-close]");
    if (closeBtn) {
      closeBtn.addEventListener("click", hideNotice);
    }

    var initialSuccess = notice.getAttribute("data-initial-success");
    var initialError = notice.getAttribute("data-initial-error");

    if (initialError && initialError !== "null") {
      showNotice(initialError, true);
    } else if (initialSuccess && initialSuccess !== "null") {
      showNotice(initialSuccess, false);
    }
  }

  document
    .querySelectorAll("form[action*='/cart/add']")
    .forEach(function (form) {
      form.addEventListener("submit", function (event) {
        event.preventDefault();

        var submitButton = form.querySelector("button[type='submit']");
        if (submitButton && submitButton.disabled) {
          showNotice("Product is out of stock.", true);
          return;
        }

        var formData = new FormData(form);
        var body = new URLSearchParams();
        formData.forEach(function (value, key) {
          body.append(key, value);
        });

        fetch(form.action, {
          method: "POST",
          headers: {
            "X-Requested-With": "XMLHttpRequest",
            Accept: "application/json",
            "Content-Type": "application/x-www-form-urlencoded;charset=UTF-8",
          },
          body: body.toString(),
        })
          .then(function (response) {
            return response
              .json()
              .catch(function () {
                return {};
              })
              .then(function (payload) {
                return {
                  ok: response.ok,
                  payload: payload,
                };
              });
          })
          .then(function (result) {
            var payload = result.payload || {};
            if (!result.ok) {
              showNotice(
                payload.message ||
                  payload.error ||
                  "Could not add item to cart.",
                true,
              );
              return;
            }

            var count = payload.cartItemCount;
            var message = payload.message || "Item added to cart.";
            if (typeof count !== "undefined") {
              message = message + " Cart items: " + count;
            }

            showNotice(message, false);
            updateCartBadges(count);
          })
          .catch(function () {
            showNotice("Could not add item to cart. Please try again.", true);
          });
      });
    });
})();
