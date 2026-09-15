```java
@Nullable
@Override
public View onCreateView(@NonNull LayoutInflater inflater,
                         @Nullable ViewGroup container,
                         @Nullable Bundle savedInstanceState) {
    binding = FragmentHomeBinding.inflate(inflater, container, false);
    return binding.getRoot();
}
```

What that function does: I inflate the home layout.
What the function is responsible for: I am the fragment view create.
Where it works: DashboardActivity.showFragment hosts me. I inflate fragment_home.xml.
Why I chose to write the code of the function the way its written: View Binding keeps ids typed.
More: I use FragmentHomeBinding.inflate.

```java
@Override
public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);

    viewModel = new ViewModelProvider(requireActivity()).get(ItemsViewModel.class);
    apiService = RetrofitClient.getApiService();
    fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext());

    nearbyAdapter = new PreviewItemAdapter(item ->
            startActivity(ItemNavigationHelper.createDetailsIntent(requireContext(), item)));

    binding.recyclerNearby.setLayoutManager(new LinearLayoutManager(
            requireContext(), LinearLayoutManager.HORIZONTAL, false));

    // Nearby list has no side padding so the fade reaches the edges.
    // Spacing between cards and at the ends is added here.
    int edgeGutter = getResources()
            .getDimensionPixelSize(R.dimen.screen_padding_horizontal);
    int itemSpacing = getResources().getDimensionPixelSize(R.dimen.space_12);
    binding.recyclerNearby.addItemDecoration(new RecyclerView.ItemDecoration() {
        @Override
        public void getItemOffsets(@NonNull Rect outRect, @NonNull View view,
                                   @NonNull RecyclerView parent,
                                   @NonNull RecyclerView.State state) {
            int position = parent.getChildAdapterPosition(view);
            boolean isLast = position == state.getItemCount() - 1;
            outRect.left = position == 0 ? edgeGutter : 0;
            outRect.right = isLast ? edgeGutter : itemSpacing;
        }
    });

    binding.recyclerNearby.setAdapter(nearbyAdapter);

    binding.btnShowMore.setOnClickListener(v -> {
        if (requireActivity() instanceof DashboardNavigator navigator)
            navigator.selectTab(R.id.nav_search);
    });

    binding.cardTargetedFound.setOnClickListener(v -> {
        if (!targetedFoundReports.isEmpty()) {
            startActivity(ItemNavigationHelper.createDetailsIntent(
                    requireContext(), targetedFoundReports.get(0)));
        }
    });

    binding.cardPendingClaims.setOnClickListener(v -> showPendingClaimsSheet());

    binding.cardClaimDecision.setOnClickListener(v -> {
        if (!unacknowledgedDecisions.isEmpty()) {
            showClaimDecisionDialog(unacknowledgedDecisions.get(0));
        }
    });

    fetchLocationIfAllowed();
    observeItems();
    AppStateEvents.observeNew(getViewLifecycleOwner(), this::refreshFromServer);
    IncomingClaimEvents.live().observe(getViewLifecycleOwner(), tick -> {
        if (tick != null) {
            loadPendingClaims();
        }
    });
    ClaimDecisionEvents.live().observe(getViewLifecycleOwner(), tick -> {
        if (tick != null) {
            loadClaimDecisions();
        }
    });
}
```

What that function does: I wire nearby cards, home notices, and live reloads.
What the function is responsible for: I am home setup.
Where it works: Fragment calls me after onCreateView. Show more uses DashboardNavigator.selectTab.
Why I chose to write the code of the function the way its written: I share ItemsViewModel with Search. Notice cards stay gone until data arrives.
More: I use PreviewItemAdapter, IncomingClaimEvents, ClaimDecisionEvents, and AppStateEvents.

```java
public void getItemOffsets(@NonNull Rect outRect, @NonNull View view,
                           @NonNull RecyclerView parent,
                           @NonNull RecyclerView.State state) {
    int position = parent.getChildAdapterPosition(view);
    boolean isLast = position == state.getItemCount() - 1;
    outRect.left = position == 0 ? edgeGutter : 0;
    outRect.right = isLast ? edgeGutter : itemSpacing;
}
```

What that function does: I add side gutters and space between nearby cards.
What the function is responsible for: I am the nearby list decoration.
Where it works: recyclerNearby calls me while laying out cards.
Why I chose to write the code of the function the way its written: The list has no padding so the fade can reach the edges. Spacing lives here instead.
More: I use RecyclerView.ItemDecoration.

```java
@Override
public void onResume() {
    super.onResume();
    refreshFromServer();
}
```

What that function does: I reload home when this tab comes back.
What the function is responsible for: I keep greeting, nearby, matches, and claim cards fresh.
Where it works: Android calls me. AppStateEvents also calls refreshFromServer.
Why I chose to write the code of the function the way its written: Returning from details or report should pick up new claims.
More: I call refreshFromServer.

```java
private void refreshFromServer() {
    viewModel.loadItems();
    loadMyItems();
    loadUserGreeting();
    loadPendingClaims();
    loadTargetedFoundReports();
    loadClaimDecisions();
}
```

What that function does: I kick off every home fetch at once.
What the function is responsible for: I am the home reload.
Where it works: I am called from onResume and AppStateEvents.observeNew.
Why I chose to write the code of the function the way its written: One method keeps all six loads together.
More: I use ItemsViewModel.loadItems and several private load methods.

```java
private void loadTargetedFoundReports() {
    String authHeader = TokenManager.getAuthHeader(requireContext());
    if (authHeader == null) return;

    apiService.getTargetedFoundReports(authHeader)
            .enqueue(new Callback<List<Item>>() {
                @Override
                public void onResponse(@NonNull Call<List<Item>> call,
                                       @NonNull Response<List<Item>> response) {
                    if (binding == null || !isAdded()) return;
                    targetedFoundReports = response.isSuccessful()
                            && response.body() != null
                            ? response.body() : new ArrayList<>();
                    renderTargetedFoundCard();
                }

                @Override
                public void onFailure(@NonNull Call<List<Item>> call,
                                      @NonNull Throwable t) {
                    if (binding == null || !isAdded()) return;
                    targetedFoundReports = new ArrayList<>();
                    renderTargetedFoundCard();
                }
            });
}
```

What that function does: I load found reports aimed at my lost items.
What the function is responsible for: I fill the private found-report card.
Where it works: I am called from refreshFromServer.
Why I chose to write the code of the function the way its written: I skip the call with no token. A failed load hides the card.
More: I use ReClaimApiService.getTargetedFoundReports.

```java
public void onResponse(@NonNull Call<List<Item>> call,
                       @NonNull Response<List<Item>> response) {
    if (binding == null || !isAdded()) return;
    targetedFoundReports = response.isSuccessful()
            && response.body() != null
            ? response.body() : new ArrayList<>();
    renderTargetedFoundCard();
}
```

What that function does: I save targeted found items and redraw the card.
What the function is responsible for: I am the success path for getTargetedFoundReports.
Where it works: Retrofit calls me from loadTargetedFoundReports.
Why I chose to write the code of the function the way its written: I drop the result if the view is gone.
More: I call renderTargetedFoundCard.

```java
public void onFailure(@NonNull Call<List<Item>> call,
                      @NonNull Throwable t) {
    if (binding == null || !isAdded()) return;
    targetedFoundReports = new ArrayList<>();
    renderTargetedFoundCard();
}
```

What that function does: I clear targeted found items on a network miss.
What the function is responsible for: I am the failure path for getTargetedFoundReports.
Where it works: Retrofit calls me from loadTargetedFoundReports.
Why I chose to write the code of the function the way its written: Hiding the card is better than a stale notice.
More: I call renderTargetedFoundCard.

```java
private void renderTargetedFoundCard() {
    if (binding == null) return;
    boolean hasTargeted = !targetedFoundReports.isEmpty();
    binding.cardTargetedFound.setVisibility(hasTargeted ? View.VISIBLE : View.GONE);
    if (hasTargeted) {
        Item latest = targetedFoundReports.get(0);
        String title = !TextUtils.isEmpty(latest.getTitle())
                ? latest.getTitle() : getString(R.string.item_details_title);
        binding.textTargetedFoundSubtitle.setText(getString(
                R.string.home_targeted_found_subtitle, title));
    }
}
```

What that function does: I show or hide the targeted found card and set its subtitle.
What the function is responsible for: I am the targeted-found UI.
Where it works: I am called from the getTargetedFoundReports callbacks.
Why I chose to write the code of the function the way its written: I use the first item as the latest. The tap handler already opens that one.
More: I use TextUtils.isEmpty for a fallback title.

```java
private void loadPendingClaims() {
    String authHeader = TokenManager.getAuthHeader(requireContext());
    if (authHeader == null) return;

    apiService.getPendingClaimsOnMyItems(authHeader)
            .enqueue(new Callback<List<PendingClaimResponse>>() {
        @Override
        public void onResponse(@NonNull Call<List<PendingClaimResponse>> call,
                               @NonNull Response<List<PendingClaimResponse>> response) {
            if (binding == null || !isAdded()) return;

            pendingClaims = response.isSuccessful() && response.body() != null
                    ? response.body() : new ArrayList<>();
            renderClaimCards();
            syncPendingClaimsSheet();
        }

        @Override
        public void onFailure(@NonNull Call<List<PendingClaimResponse>> call,
                              @NonNull Throwable t) {
            if (binding == null || !isAdded()) return;
            pendingClaims = new ArrayList<>();
            renderClaimCards();
            syncPendingClaimsSheet();
        }
    });
}
```

What that function does: I load claims waiting on my items.
What the function is responsible for: I fill the pending-claims card and any open sheet.
Where it works: I am called from refreshFromServer and IncomingClaimEvents.
Why I chose to write the code of the function the way its written: If the sheet is already open, I refresh it instead of leaving stale rows.
More: I use getPendingClaimsOnMyItems.

```java
public void onResponse(@NonNull Call<List<PendingClaimResponse>> call,
                       @NonNull Response<List<PendingClaimResponse>> response) {
    if (binding == null || !isAdded()) return;

    pendingClaims = response.isSuccessful() && response.body() != null
            ? response.body() : new ArrayList<>();
    renderClaimCards();
    syncPendingClaimsSheet();
}
```

What that function does: I save pending claims and update the card and sheet.
What the function is responsible for: I am the success path for getPendingClaimsOnMyItems.
Where it works: Retrofit calls me from loadPendingClaims.
Why I chose to write the code of the function the way its written: I keep the open sheet in sync with the new list.
More: I call renderClaimCards and syncPendingClaimsSheet.

```java
public void onFailure(@NonNull Call<List<PendingClaimResponse>> call,
                      @NonNull Throwable t) {
    if (binding == null || !isAdded()) return;
    pendingClaims = new ArrayList<>();
    renderClaimCards();
    syncPendingClaimsSheet();
}
```

What that function does: I clear pending claims on a miss.
What the function is responsible for: I am the failure path for getPendingClaimsOnMyItems.
Where it works: Retrofit calls me from loadPendingClaims.
Why I chose to write the code of the function the way its written: An empty list hides the card and closes the sheet.
More: I call renderClaimCards and syncPendingClaimsSheet.

```java
private void showPendingClaimsSheet() {
    if (pendingClaims.isEmpty()) {
        return;
    }
    if (pendingClaimsSheet != null && pendingClaimsSheet.isShowing()) {
        pendingClaimsSheet.replaceClaims(pendingClaims);
        return;
    }
    pendingClaimsSheet = PendingClaimsSheet.show(this, pendingClaims,
            new PendingClaimsSheet.Listener() {
                @Override
                public void onClaimsChanged(@NonNull List<PendingClaimResponse> remaining) {
                    pendingClaims = new ArrayList<>(remaining);
                    renderClaimCards();
                }

                @Override
                public void onSheetDismissed() {
                    pendingClaimsSheet = null;
                    if (binding != null && isAdded()) {
                        refreshFromServer();
                    }
                }
            });
}
```

What that function does: I open the pending-claims sheet, or I refresh it if it is already up.
What the function is responsible for: I am the pending-claims card tap.
Where it works: I am called from cardPendingClaims. PendingClaimsSheet inflates dialog_pending_claims.xml.
Why I chose to write the code of the function the way its written: Replacing claims avoids stacking two sheets.
More: I use PendingClaimsSheet.show.

```java
public void onClaimsChanged(@NonNull List<PendingClaimResponse> remaining) {
    pendingClaims = new ArrayList<>(remaining);
    renderClaimCards();
}
```

What that function does: I keep the home list in step after a claim is reviewed in the sheet.
What the function is responsible for: I am the sheet list-changed callback.
Where it works: PendingClaimsSheet.Listener calls me.
Why I chose to write the code of the function the way its written: Copying the list avoids sharing a live mutable list with the sheet.
More: I call renderClaimCards.

```java
public void onSheetDismissed() {
    pendingClaimsSheet = null;
    if (binding != null && isAdded()) {
        refreshFromServer();
    }
}
```

What that function does: I drop the sheet pointer and reload home.
What the function is responsible for: I am the sheet dismiss callback.
Where it works: PendingClaimsSheet.Listener calls me.
Why I chose to write the code of the function the way its written: A reload picks up counts after approve or reject.
More: I call refreshFromServer.

```java
private void syncPendingClaimsSheet() {
    if (pendingClaimsSheet == null || !pendingClaimsSheet.isShowing()) {
        return;
    }
    if (pendingClaims.isEmpty()) {
        pendingClaimsSheet.dismiss();
        return;
    }
    pendingClaimsSheet.replaceClaims(pendingClaims);
}
```

What that function does: I update or close the open sheet when the list changes.
What the function is responsible for: I keep the sheet honest with the server.
Where it works: I am called from the pending-claims callbacks.
Why I chose to write the code of the function the way its written: An empty list should close the sheet by itself.
More: I use dismiss and replaceClaims.

```java
private void renderClaimCards() {
    if (binding == null) return;

    int count = pendingClaims.size();
    boolean hasClaims = count > 0;

    binding.cardPendingClaims.setVisibility(hasClaims ? View.VISIBLE : View.GONE);
    if (hasClaims) {
        binding.textPendingClaimsCount.setText(getString(
                R.string.home_pending_claims_subtitle, count));
    }
}
```

What that function does: I show the pending-claims card with a count, or I hide it.
What the function is responsible for: I am the pending-claims badge.
Where it works: I am called from loadPendingClaims, showPendingClaimsSheet's listener, and related paths.
Why I chose to write the code of the function the way its written: Gone when empty keeps home quiet.
More: I use home_pending_claims_subtitle.

```java
private void loadClaimDecisions() {
    String authHeader = TokenManager.getAuthHeader(requireContext());
    if (authHeader == null) return;

    apiService.getMyClaims(authHeader).enqueue(new Callback<List<Claim>>() {
        @Override
        public void onResponse(@NonNull Call<List<Claim>> call,
                               @NonNull Response<List<Claim>> response) {
            if (binding == null || !isAdded()) return;
            unacknowledgedDecisions = new ArrayList<>();
            if (response.isSuccessful() && response.body() != null) {
                for (Claim claim : response.body()) {
                    if (claim.isDecisionAcknowledged()) {
                        continue;
                    }
                    String status = claim.getStatus();
                    if ("APPROVED".equalsIgnoreCase(status)
                            || "REJECTED".equalsIgnoreCase(status)) {
                        unacknowledgedDecisions.add(claim);
                    }
                }
            }
            renderDecisionCard();
            syncClaimDecisionDialog();
        }

        @Override
        public void onFailure(@NonNull Call<List<Claim>> call, @NonNull Throwable t) {
            if (binding == null || !isAdded()) return;
            unacknowledgedDecisions = new ArrayList<>();
            renderDecisionCard();
            syncClaimDecisionDialog();
        }
    });
}
```

What that function does: I load my claims and keep the ones I have not confirmed yet.
What the function is responsible for: I fill the claim-decision card.
Where it works: I am called from refreshFromServer and ClaimDecisionEvents.
Why I chose to write the code of the function the way its written: Pending claims stay off this card. Only approved or rejected, and not yet acknowledged.
More: I use getMyClaims.

```java
public void onResponse(@NonNull Call<List<Claim>> call,
                       @NonNull Response<List<Claim>> response) {
    if (binding == null || !isAdded()) return;
    unacknowledgedDecisions = new ArrayList<>();
    if (response.isSuccessful() && response.body() != null) {
        for (Claim claim : response.body()) {
            if (claim.isDecisionAcknowledged()) {
                continue;
            }
            String status = claim.getStatus();
            if ("APPROVED".equalsIgnoreCase(status)
                    || "REJECTED".equalsIgnoreCase(status)) {
                unacknowledgedDecisions.add(claim);
            }
        }
    }
    renderDecisionCard();
    syncClaimDecisionDialog();
}
```

What that function does: I filter unacknowledged decisions and redraw.
What the function is responsible for: I am the success path for getMyClaims.
Where it works: Retrofit calls me from loadClaimDecisions.
Why I chose to write the code of the function the way its written: I rebuild the list each time so stale claims drop off after confirm.
More: I call renderDecisionCard and syncClaimDecisionDialog.

```java
public void onFailure(@NonNull Call<List<Claim>> call, @NonNull Throwable t) {
    if (binding == null || !isAdded()) return;
    unacknowledgedDecisions = new ArrayList<>();
    renderDecisionCard();
    syncClaimDecisionDialog();
}
```

What that function does: I clear decisions on a miss.
What the function is responsible for: I am the failure path for getMyClaims.
Where it works: Retrofit calls me from loadClaimDecisions.
Why I chose to write the code of the function the way its written: I hide the card rather than keep a stale notice.
More: I call renderDecisionCard.

```java
private void renderDecisionCard() {
    if (binding == null) return;

    boolean hasDecision = !unacknowledgedDecisions.isEmpty();
    binding.cardClaimDecision.setVisibility(hasDecision ? View.VISIBLE : View.GONE);
    if (!hasDecision) {
        return;
    }
    Claim latest = unacknowledgedDecisions.get(0);
    boolean approved = "APPROVED".equalsIgnoreCase(latest.getStatus());
    binding.textClaimDecisionTitle.setText(approved
            ? R.string.home_claim_decision_title_approved
            : R.string.home_claim_decision_title_declined);
    String title = latest.getItemTitle();
    binding.textClaimDecisionSubtitle.setText(
            !TextUtils.isEmpty(title) ? title : getString(R.string.home_claim_decision_subtitle));
}
```

What that function does: I show the latest unconfirmed decision on the home card.
What the function is responsible for: I am the decision-card UI.
Where it works: I am called after loadClaimDecisions and acknowledgeDecision.
Why I chose to write the code of the function the way its written: Approved and declined use different titles. The item title is the subtitle.
More: I compare status with equalsIgnoreCase.

```java
private void showClaimDecisionDialog(Claim claim) {
    decidingClaimId = claim.getId();
    boolean approved = "APPROVED".equalsIgnoreCase(claim.getStatus());
    String itemTitle = !TextUtils.isEmpty(claim.getItemTitle())
            ? claim.getItemTitle() : getString(R.string.item_details_title);
    int titleRes = approved
            ? R.string.home_claim_decision_title_approved
            : R.string.home_claim_decision_title_declined;
    String body = getString(approved
                    ? R.string.claim_decision_approved_body
                    : R.string.claim_decision_declined_body,
            itemTitle);

    MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext())
            .setTitle(titleRes)
            .setMessage(body)
            .setPositiveButton(R.string.btn_confirm, (d, w) ->
                    acknowledgeDecision(claim.getId()));

    if (approved) {
        builder.setNeutralButton(R.string.btn_view_contact_info, null);
    }

    claimDecisionDialog = builder.create();
    claimDecisionDialog.setOnShowListener(d -> {
        if (!approved) {
            return;
        }
        android.widget.Button contact = claimDecisionDialog.getButton(
                androidx.appcompat.app.AlertDialog.BUTTON_NEUTRAL);
        contact.setOnClickListener(v -> showOwnerContact(claim.getId()));
    });
    claimDecisionDialog.setOnDismissListener(d -> {
        claimDecisionDialog = null;
        decidingClaimId = null;
    });
    claimDecisionDialog.show();
}
```

What that function does: I show the owner's decision. Confirm hides it. Approve also offers contact.
What the function is responsible for: I am the decision dialog.
Where it works: I am called from cardClaimDecision. Contact uses ContactInfoDialog.
Why I chose to write the code of the function the way its written: Neutral contact is set after show so the dialog stays open while contact loads.
More: I use MaterialAlertDialogBuilder.

```java
private void syncClaimDecisionDialog() {
    if (claimDecisionDialog == null || !claimDecisionDialog.isShowing()
            || decidingClaimId == null) {
        return;
    }
    Claim match = null;
    for (Claim claim : unacknowledgedDecisions) {
        if (decidingClaimId.equals(claim.getId())) {
            match = claim;
            break;
        }
    }
    if (match == null) {
        claimDecisionDialog.dismiss();
        return;
    }
    boolean approved = "APPROVED".equalsIgnoreCase(match.getStatus());
    String itemTitle = !TextUtils.isEmpty(match.getItemTitle())
            ? match.getItemTitle() : getString(R.string.item_details_title);
    claimDecisionDialog.setTitle(getString(approved
            ? R.string.home_claim_decision_title_approved
            : R.string.home_claim_decision_title_declined));
    claimDecisionDialog.setMessage(getString(approved
                    ? R.string.claim_decision_approved_body
                    : R.string.claim_decision_declined_body,
            itemTitle));
}
```

What that function does: I refresh or close the open decision dialog after a reload.
What the function is responsible for: I keep the dialog honest with the server.
Where it works: I am called from the getMyClaims callbacks.
Why I chose to write the code of the function the way its written: If that claim is gone, I close the dialog.
More: I use setTitle and setMessage on AlertDialog.

```java
private void acknowledgeDecision(String claimId) {
    String authHeader = TokenManager.getAuthHeader(requireContext());
    if (authHeader == null) return;

    unacknowledgedDecisions.removeIf(c -> claimId.equals(c.getId()));
    renderDecisionCard();

    apiService.acknowledgeClaimDecision(authHeader, claimId)
            .enqueue(new Callback<Claim>() {
                @Override
                public void onResponse(@NonNull Call<Claim> call,
                                       @NonNull Response<Claim> response) {
                    if (binding == null || !isAdded()) return;
                    loadClaimDecisions();
                }

                @Override
                public void onFailure(@NonNull Call<Claim> call, @NonNull Throwable t) {
                    if (binding == null || !isAdded()) return;
                    loadClaimDecisions();
                }
            });
}
```

What that function does: I hide this decision now, then I tell the server I saw it.
What the function is responsible for: I am confirm on the decision dialog.
Where it works: The dialog positive button calls me.
Why I chose to write the code of the function the way its written: Removing locally first makes the card drop right away. Reload puts it back if the server call failed.
More: I use acknowledgeClaimDecision.

```java
public void onResponse(@NonNull Call<Claim> call,
                       @NonNull Response<Claim> response) {
    if (binding == null || !isAdded()) return;
    loadClaimDecisions();
}
```

What that function does: I reload decisions after confirm.
What the function is responsible for: I am the success path for acknowledgeClaimDecision.
Where it works: Retrofit calls me from acknowledgeDecision.
Why I chose to write the code of the function the way its written: A reload is safer than trusting one claim body.
More: I call loadClaimDecisions.

```java
public void onFailure(@NonNull Call<Claim> call, @NonNull Throwable t) {
    if (binding == null || !isAdded()) return;
    loadClaimDecisions();
}
```

What that function does: I reload decisions if confirm failed.
What the function is responsible for: I am the failure path for acknowledgeClaimDecision.
Where it works: Retrofit calls me from acknowledgeDecision.
Why I chose to write the code of the function the way its written: Reloading puts the card back if the server never got the confirm.
More: I call loadClaimDecisions.

```java
private void showOwnerContact(String claimId) {
    String authHeader = TokenManager.getAuthHeader(requireContext());
    if (authHeader == null) return;

    apiService.getClaimContact(authHeader, claimId)
            .enqueue(new Callback<ContactResponse>() {
                @Override
                public void onResponse(@NonNull Call<ContactResponse> call,
                                       @NonNull Response<ContactResponse> response) {
                    if (!isAdded()) return;
                    if (response.isSuccessful() && response.body() != null) {
                        ContactInfoDialog.show(requireContext(), response.body(),
                                getViewLifecycleOwner(),
                                callback -> {
                                    String header = TokenManager.getAuthHeader(requireContext());
                                    if (header == null) {
                                        return;
                                    }
                                    apiService.getClaimContact(header, claimId).enqueue(callback);
                                });
                    } else {
                        Toast.makeText(requireContext(),
                                R.string.msg_contact_load_failed, Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(@NonNull Call<ContactResponse> call,
                                      @NonNull Throwable t) {
                    if (!isAdded()) return;
                    Toast.makeText(requireContext(),
                            R.string.msg_contact_load_failed, Toast.LENGTH_SHORT).show();
                }
            });
}
```

What that function does: I load the other person's contact after an approved claim.
What the function is responsible for: I am the view-contact button.
Where it works: The decision dialog's contact button calls me. ContactInfoDialog inflates dialog_contact_info.xml.
Why I chose to write the code of the function the way its written: The dialog can refresh contact itself through the callback I pass.
More: I use getClaimContact and ContactInfoDialog.show.

```java
public void onResponse(@NonNull Call<ContactResponse> call,
                       @NonNull Response<ContactResponse> response) {
    if (!isAdded()) return;
    if (response.isSuccessful() && response.body() != null) {
        ContactInfoDialog.show(requireContext(), response.body(),
                getViewLifecycleOwner(),
                callback -> {
                    String header = TokenManager.getAuthHeader(requireContext());
                    if (header == null) {
                        return;
                    }
                    apiService.getClaimContact(header, claimId).enqueue(callback);
                });
    } else {
        Toast.makeText(requireContext(),
                R.string.msg_contact_load_failed, Toast.LENGTH_SHORT).show();
    }
}
```

What that function does: I open the contact dialog, or I toast if the body is missing.
What the function is responsible for: I am the success path for getClaimContact.
Where it works: Retrofit calls me from showOwnerContact.
Why I chose to write the code of the function the way its written: Passing a refetch callback lets the dialog retry without HomeFragment knowing the dialog internals.
More: I use ContactInfoDialog.show.

```java
public void onFailure(@NonNull Call<ContactResponse> call,
                      @NonNull Throwable t) {
    if (!isAdded()) return;
    Toast.makeText(requireContext(),
            R.string.msg_contact_load_failed, Toast.LENGTH_SHORT).show();
}
```

What that function does: I toast if contact cannot load.
What the function is responsible for: I am the failure path for getClaimContact.
Where it works: Retrofit calls me from showOwnerContact.
Why I chose to write the code of the function the way its written: A short toast is enough. The decision dialog can stay open.
More: I use Toast.LENGTH_SHORT.

```java
private void loadUserGreeting() {
    String authHeader = TokenManager.getAuthHeader(requireContext());
    if (authHeader == null) return;

    apiService.getCurrentUser(authHeader).enqueue(new Callback<User>() {
        @Override
        public void onResponse(@NonNull Call<User> call, @NonNull Response<User> response) {
            if (binding == null || !isAdded()) return;

            if (response.isSuccessful() && response.body() != null) {
                String name = response.body().getName();

                if (name != null && !name.isEmpty())
                    binding.textGreeting.setText(getString(R.string.dashboard_greeting_named, name));

                loadAvatar(response.body().getProfilePictureUrl());
            }
        }

        @Override
        public void onFailure(@NonNull Call<User> call, @NonNull Throwable t) {
            // Keep the default greeting
        }
    });
}
```

What that function does: I load the signed-in user for the hello text and avatar.
What the function is responsible for: I am the home greeting.
Where it works: I am called from refreshFromServer.
Why I chose to write the code of the function the way its written: A failed call leaves the default Hello from XML.
More: I use getCurrentUser.

```java
public void onResponse(@NonNull Call<User> call, @NonNull Response<User> response) {
    if (binding == null || !isAdded()) return;

    if (response.isSuccessful() && response.body() != null) {
        String name = response.body().getName();

        if (name != null && !name.isEmpty())
            binding.textGreeting.setText(getString(R.string.dashboard_greeting_named, name));

        loadAvatar(response.body().getProfilePictureUrl());
    }
}
```

What that function does: I set the named greeting and start the avatar load.
What the function is responsible for: I am the success path for getCurrentUser.
Where it works: Retrofit calls me from loadUserGreeting.
Why I chose to write the code of the function the way its written: An empty name keeps the XML default.
More: I call loadAvatar.

```java
public void onFailure(@NonNull Call<User> call, @NonNull Throwable t) {
    // Keep the default greeting
}
```

What that function does: I leave the default greeting if the user call fails.
What the function is responsible for: I am the failure path for getCurrentUser.
Where it works: Retrofit calls me from loadUserGreeting.
Why I chose to write the code of the function the way its written: Home still works without a name.
More: I leave the body empty.

```java
private void loadAvatar(@Nullable String url) {
    if (binding == null || url == null || url.isEmpty()) return;

    binding.imageHomeAvatar.setPadding(0, 0, 0, 0);
    binding.imageHomeAvatar.setImageTintList(null);
    Glide.with(this)
            .load(url)
            .circleCrop()
            .placeholder(R.drawable.ic_settings_person)
            .error(R.drawable.ic_settings_person)
            .into(binding.imageHomeAvatar);
}
```

What that function does: I load the profile photo into the home avatar.
What the function is responsible for: I am the greeting picture.
Where it works: I am called from loadUserGreeting's onResponse.
Why I chose to write the code of the function the way its written: I drop padding and tint so a real photo is not shrunk or colored like the placeholder icon.
More: I use Glide.circleCrop.

```java
private void loadMyItems() {
    String authHeader = TokenManager.getAuthHeader(requireContext());
    if (authHeader == null) return;

    apiService.getMyItems(authHeader).enqueue(new Callback<List<Item>>() {
        @Override
        public void onResponse(@NonNull Call<List<Item>> call,
                               @NonNull Response<List<Item>> response) {
            if (binding == null || !isAdded()) return;

            if (response.isSuccessful() && response.body() != null) {
                myItems = response.body();
                renderMatches(viewModel.getItems().getValue());
            }
        }

        @Override
        public void onFailure(@NonNull Call<List<Item>> call, @NonNull Throwable t) {
            // Leave matches empty
        }
    });
}
```

What that function does: I load my reports so I can match them against public items.
What the function is responsible for: I fill myItems for the matches section.
Where it works: I am called from refreshFromServer.
Why I chose to write the code of the function the way its written: Matches need both my lost items and the public list from the ViewModel.
More: I use getMyItems.

```java
public void onResponse(@NonNull Call<List<Item>> call,
                       @NonNull Response<List<Item>> response) {
    if (binding == null || !isAdded()) return;

    if (response.isSuccessful() && response.body() != null) {
        myItems = response.body();
        renderMatches(viewModel.getItems().getValue());
    }
}
```

What that function does: I save my items and redraw matches.
What the function is responsible for: I am the success path for getMyItems.
Where it works: Retrofit calls me from loadMyItems.
Why I chose to write the code of the function the way its written: I pass the current ViewModel list so matches do not wait for another emit.
More: I call renderMatches.

```java
public void onFailure(@NonNull Call<List<Item>> call, @NonNull Throwable t) {
    // Leave matches empty
}
```

What that function does: I leave matches as they are if my items fail to load.
What the function is responsible for: I am the failure path for getMyItems.
Where it works: Retrofit calls me from loadMyItems.
Why I chose to write the code of the function the way its written: An empty myItems list already shows the empty matches text.
More: I leave the body empty.

```java
private void fetchLocationIfAllowed() {
    if (ContextCompat.checkSelfPermission(requireContext(),
            Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
        fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
            if (location != null) {
                userLatitude = location.getLatitude();
                userLongitude = location.getLongitude();
                renderNearby(viewModel.getItems().getValue());
            }
        });
    }
}
```

What that function does: I read last location if permission is already granted.
What the function is responsible for: I sort nearby by distance when I can.
Where it works: I am called from onViewCreated.
Why I chose to write the code of the function the way its written: I do not prompt here. Search already asks when the user sorts by distance.
More: I use FusedLocationProviderClient.getLastLocation.

```java
private void observeItems() {
    viewModel.getLoading().observe(getViewLifecycleOwner(), loading -> {
        if (loading != null && binding != null)
            binding.progressHome.setVisibility(loading ? View.VISIBLE : View.GONE);
    });

    viewModel.getErrorMessage().observe(getViewLifecycleOwner(), message -> {
        if (message != null && !message.isEmpty())
            ConnectionToasts.showIfOnline(requireContext(), message, Toast.LENGTH_SHORT);
    });

    viewModel.getItems().observe(getViewLifecycleOwner(), items -> {
        renderNearby(items);
        renderMatches(items);
    });
}
```

What that function does: I watch the shared item list, loading, and errors.
What the function is responsible for: I am the ViewModel observer setup.
Where it works: I am called from onViewCreated.
Why I chose to write the code of the function the way its written: One observe block keeps nearby and matches on the same list.
More: I use ConnectionToasts.showIfOnline.

```java
private void renderNearby(List<Item> items) {
    if (binding == null) return;

    List<Item> source = items != null ? items : new ArrayList<>();
    List<Item> preview = ItemMatchHelper.nearbyPreview(
            source, userLatitude, userLongitude, NEARBY_PREVIEW_LIMIT);

    nearbyAdapter.updateItems(preview);
    boolean empty = preview.isEmpty();

    binding.textNearbyEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
    binding.recyclerNearby.setVisibility(empty ? View.GONE : View.VISIBLE);
    binding.btnShowMore.setVisibility(empty ? View.GONE : View.VISIBLE);
}
```

What that function does: I show up to four nearby items, or the empty text.
What the function is responsible for: I am the nearby row.
Where it works: I am called from observeItems and fetchLocationIfAllowed.
Why I chose to write the code of the function the way its written: A short preview keeps home light. Show more jumps to Search.
More: I use ItemMatchHelper.nearbyPreview with NEARBY_PREVIEW_LIMIT 4.

```java
private void renderMatches(List<Item> allItems) {
    if (binding == null) return;

    binding.layoutMatches.removeAllViews();
    List<Item> lostItems = new ArrayList<>();

    for (Item item : myItems) {
        if ("LOST".equalsIgnoreCase(item.getType())
                && "OPEN".equalsIgnoreCase(item.getStatus())) {
            lostItems.add(item);
        }
    }

    String userId = TokenManager.getUserId(requireContext());

    List<ItemMatchHelper.MatchGroup> groups = ItemMatchHelper.buildMatchGroups(
            allItems != null ? allItems : new ArrayList<>(), lostItems, userId);

    if (groups.isEmpty()) {
        binding.textMatchesEmpty.setVisibility(View.VISIBLE);
        return;
    }

    binding.textMatchesEmpty.setVisibility(View.GONE);
    LayoutInflater inflater = LayoutInflater.from(requireContext());

    for (ItemMatchHelper.MatchGroup group : groups) {
        MaterialTextView header = new MaterialTextView(requireContext());

        header.setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_TitleSmall);
        header.setTextColor(requireContext().getColor(R.color.brand_dark));
        header.setPadding(0,
                getResources().getDimensionPixelSize(R.dimen.space_16),
                0,
                getResources().getDimensionPixelSize(R.dimen.space_10));
        header.setText(getString(R.string.home_match_for, group.lostItem.getTitle()));

        binding.layoutMatches.addView(header);

        androidx.recyclerview.widget.RecyclerView recycler =
                new androidx.recyclerview.widget.RecyclerView(requireContext());

        recycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        recycler.setNestedScrollingEnabled(false);

        ItemAdapter adapter = new ItemAdapter(group.matches, item ->
                startActivity(ItemNavigationHelper.createDetailsIntent(requireContext(), item)));

        recycler.setAdapter(adapter);
        binding.layoutMatches.addView(recycler);
    }
}
```

What that function does: I build a header and list for each of my open lost items that has matches.
What the function is responsible for: I am the possible-matches section.
Where it works: I am called from observeItems and loadMyItems.
Why I chose to write the code of the function the way its written: Groups are built in code because the count is not known in XML. Nested scroll is off so the parent NestedScrollView owns scrolling.
More: I use ItemMatchHelper.buildMatchGroups and ItemAdapter. inflater is unused after I switched to code-built views.

```java
@Override
public void onDestroyView() {
    if (claimDecisionDialog != null) {
        claimDecisionDialog.dismiss();
    }
    if (pendingClaimsSheet != null) {
        pendingClaimsSheet.dismissQuietly();
        pendingClaimsSheet = null;
    }
    super.onDestroyView();
    binding = null;
}
```

What that function does: I close dialogs and drop the binding when the view is destroyed.
What the function is responsible for: I stop leaking a dialog across a tab change.
Where it works: Fragment calls me.
Why I chose to write the code of the function the way its written: dismissQuietly avoids a dismiss callback that would reload after the view is gone.
More: I set binding to null.
