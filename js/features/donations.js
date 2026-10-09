import { state } from "../core/state.js";

export function openDonation() {
  const url = state.config.donateUrl || "";
  if (url) window.open(url, "_blank", "noopener,noreferrer");
}
