```java
@Override
protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    binding = ActivityReportHistoryBinding.inflate(getLayoutInflater());
    setContentView(binding.getRoot());

    apiService = RetrofitClient.getApiService();

    setSupportActionBar(binding.toolbarReportHistory);
    if (getSupportActionBar() != null) {
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }
    binding.toolbarReportHistory.setNavigationOnClickListener(v -> finish());

    adapter = new HistoryAdapter(this::showOwnerContact);
    binding.recyclerReportHistory.setLayoutManager(new LinearLayoutManager(this));
    binding.recyclerReportHistory.setAdapter(adapter);

    AppDataRefresh.observeNew(this, this::loadHistory);
}
```

What that function does: I show the history list and wire contact taps.
What the function is responsible for: I am the report-history screen start.
Where it works: ProfileSettingsFragment starts me. I inflate activity_report_history.xml. Rows use item_history.xml.
Why I chose to write the code of the function the way its written: Back finishes me. AppDataRefresh reloads after a write.
More: I use HistoryAdapter and LinearLayoutManager.

```java
@Override
protected void onResume() {
    super.onResume();
    loadHistory();
}
```

What that function does: I reload history whenever this screen comes back.
What the function is responsible for: I keep the list fresh.
Where it works: Android calls me.
Why I chose to write the code of the function the way its written: onResume covers returning from other screens.
More: I call loadHistory.

```java
private void loadHistory() {
    String authHeader = TokenManager.getAuthHeader(this);
    if (authHeader == null) {
        finish();
        return;
    }

    reportEntries.clear();
    claimEntries.clear();
    pendingRequests = 2;
    binding.progressReportHistory.setVisibility(View.VISIBLE);
    binding.textHistoryEmpty.setVisibility(View.GONE);

    apiService.getMyItems(authHeader).enqueue(new Callback<List<Item>>() {
        @Override
        public void onResponse(@NonNull Call<List<Item>> call,
                               @NonNull Response<List<Item>> response) {
            if (response.isSuccessful() && response.body() != null) {
                for (Item item : response.body()) {
                    reportEntries.add(new HistoryAdapter.Entry(
                            item.getTitle(),
                            item.getImageUrl(),
                            R.string.history_role_reported,
                            statusForReport(item)));
                }
            }
            onSectionLoaded();
        }

        @Override
        public void onFailure(@NonNull Call<List<Item>> call, @NonNull Throwable t) {
            onSectionLoaded();
        }
    });

    apiService.getMyClaims(authHeader).enqueue(new Callback<List<Claim>>() {
        @Override
        public void onResponse(@NonNull Call<List<Claim>> call,
                               @NonNull Response<List<Claim>> response) {
            if (response.isSuccessful() && response.body() != null) {
                for (Claim claim : response.body()) {
                    HistoryAdapter.Status status = statusForClaim(claim);
                    boolean approvedClaim = status == HistoryAdapter.Status.APPROVED;
                    claimEntries.add(new HistoryAdapter.Entry(
                            claim.getItemTitle(),
                            claim.getItemImageUrl(),
                            R.string.history_role_claimed,
                            status,
                            claim.getId(),
                            approvedClaim));
                }
            }
            onSectionLoaded();
        }

        @Override
        public void onFailure(@NonNull Call<List<Claim>> call, @NonNull Throwable t) {
            onSectionLoaded();
        }
    });
}
```

What that function does: I fetch my reports and my claims, then I wait for both before drawing.
What the function is responsible for: I am the history load.
Where it works: I am called from onResume and AppDataRefresh.
Why I chose to write the code of the function the way its written: Two calls finish through onSectionLoaded so the list draws once. Contact is only on approved claims.
More: I use getMyItems and getMyClaims. pendingRequests starts at 2.

```java
public void onResponse(@NonNull Call<List<Item>> call,
                       @NonNull Response<List<Item>> response) {
    if (response.isSuccessful() && response.body() != null) {
        for (Item item : response.body()) {
            reportEntries.add(new HistoryAdapter.Entry(
                    item.getTitle(),
                    item.getImageUrl(),
                    R.string.history_role_reported,
                    statusForReport(item)));
        }
    }
    onSectionLoaded();
}
```

What that function does: I turn my reports into history rows, then I mark this half done.
What the function is responsible for: I am the Retrofit success path for getMyItems.
Where it works: Retrofit calls me from loadHistory.
Why I chose to write the code of the function the way its written: I always call onSectionLoaded so a failed body still unblocks the spinner.
More: I use HistoryAdapter.Entry and statusForReport.

```java
public void onFailure(@NonNull Call<List<Item>> call, @NonNull Throwable t) {
    onSectionLoaded();
}
```

What that function does: I mark the reports half done without adding rows.
What the function is responsible for: I am the Retrofit failure path for getMyItems.
Where it works: Retrofit calls me from loadHistory.
Why I chose to write the code of the function the way its written: Claims can still show if reports fail.
More: I call onSectionLoaded.

```java
public void onResponse(@NonNull Call<List<Claim>> call,
                       @NonNull Response<List<Claim>> response) {
    if (response.isSuccessful() && response.body() != null) {
        for (Claim claim : response.body()) {
            HistoryAdapter.Status status = statusForClaim(claim);
            boolean approvedClaim = status == HistoryAdapter.Status.APPROVED;
            claimEntries.add(new HistoryAdapter.Entry(
                    claim.getItemTitle(),
                    claim.getItemImageUrl(),
                    R.string.history_role_claimed,
                    status,
                    claim.getId(),
                    approvedClaim));
        }
    }
    onSectionLoaded();
}
```

What that function does: I turn my claims into history rows, with contact on approved ones.
What the function is responsible for: I am the Retrofit success path for getMyClaims.
Where it works: Retrofit calls me from loadHistory.
Why I chose to write the code of the function the way its written: showContact is true only for APPROVED so the button stays hidden otherwise.
More: I use claim.getId and statusForClaim.

```java
public void onFailure(@NonNull Call<List<Claim>> call, @NonNull Throwable t) {
    onSectionLoaded();
}
```

What that function does: I mark the claims half done without adding rows.
What the function is responsible for: I am the Retrofit failure path for getMyClaims.
Where it works: Retrofit calls me from loadHistory.
Why I chose to write the code of the function the way its written: Reports can still show if claims fail.
More: I call onSectionLoaded.

```java
private void onSectionLoaded() {
    if (binding == null || isFinishing()) {
        return;
    }
    pendingRequests--;
    if (pendingRequests > 0) {
        return;
    }

    binding.progressReportHistory.setVisibility(View.GONE);

    List<HistoryAdapter.Entry> combined = new ArrayList<>();
    combined.addAll(reportEntries);
    combined.addAll(claimEntries);
    adapter.submit(combined);

    binding.textHistoryEmpty.setVisibility(
            combined.isEmpty() ? View.VISIBLE : View.GONE);
}
```

What that function does: I draw the combined list when both fetches finish.
What the function is responsible for: I am the join point for the two API calls.
Where it works: I am called from both callbacks.
Why I chose to write the code of the function the way its written: pendingRequests avoids drawing twice. Reports come first, then claims.
More: I use adapter.submit.

```java
private HistoryAdapter.Status statusForReport(Item item) {
    return "CLOSED".equalsIgnoreCase(item.getStatus())
            ? HistoryAdapter.Status.APPROVED
            : HistoryAdapter.Status.PENDING;
}
```

What that function does: I map a closed report to Approved and an open one to Pending.
What the function is responsible for: I pick the report badge.
Where it works: I am called while building reportEntries.
Why I chose to write the code of the function the way its written: History only has three badges, so closed reports reuse Approved.
More: I use Item.getStatus.

```java
private HistoryAdapter.Status statusForClaim(Claim claim) {
    String status = claim.getStatus();
    if ("APPROVED".equalsIgnoreCase(status)) {
        return HistoryAdapter.Status.APPROVED;
    }
    if ("REJECTED".equalsIgnoreCase(status)) {
        return HistoryAdapter.Status.DECLINED;
    }
    return HistoryAdapter.Status.PENDING;
}
```

What that function does: I map claim status to the history badge.
What the function is responsible for: I pick the claim badge.
Where it works: I am called while building claimEntries.
Why I chose to write the code of the function the way its written: Rejected is shown as Declined. Anything else is Pending.
More: I use Claim.getStatus.

```java
private void showOwnerContact(String claimId) {
    String authHeader = TokenManager.getAuthHeader(this);
    if (authHeader == null) {
        return;
    }
    apiService.getClaimContact(authHeader, claimId)
            .enqueue(new Callback<ContactResponse>() {
                @Override
                public void onResponse(@NonNull Call<ContactResponse> call,
                                       @NonNull Response<ContactResponse> response) {
                    if (isFinishing()) {
                        return;
                    }
                    if (response.isSuccessful() && response.body() != null) {
                        ContactInfoDialog.show(ReportHistoryActivity.this, response.body(),
                                ReportHistoryActivity.this,
                                callback -> {
                                    String header = TokenManager.getAuthHeader(
                                            ReportHistoryActivity.this);
                                    if (header == null) {
                                        return;
                                    }
                                    apiService.getClaimContact(header, claimId).enqueue(callback);
                                });
                    } else {
                        Toast.makeText(ReportHistoryActivity.this,
                                R.string.msg_contact_load_failed, Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(@NonNull Call<ContactResponse> call,
                                      @NonNull Throwable t) {
                    if (isFinishing()) {
                        return;
                    }
                    Toast.makeText(ReportHistoryActivity.this,
                            R.string.msg_contact_load_failed, Toast.LENGTH_SHORT).show();
                }
            });
}
```

What that function does: I load claim contact and show the dialog, with a reloader if data changes.
What the function is responsible for: I am the history contact button path.
Where it works: HistoryAdapter calls me for approved claims.
Why I chose to write the code of the function the way its written: The reloader repeats getClaimContact so the dialog can refresh.
More: I use ContactInfoDialog.show and apiService.getClaimContact.

```java
public void onResponse(@NonNull Call<ContactResponse> call,
                       @NonNull Response<ContactResponse> response) {
    if (isFinishing()) {
        return;
    }
    if (response.isSuccessful() && response.body() != null) {
        ContactInfoDialog.show(ReportHistoryActivity.this, response.body(),
                ReportHistoryActivity.this,
                callback -> {
                    String header = TokenManager.getAuthHeader(
                            ReportHistoryActivity.this);
                    if (header == null) {
                        return;
                    }
                    apiService.getClaimContact(header, claimId).enqueue(callback);
                });
    } else {
        Toast.makeText(ReportHistoryActivity.this,
                R.string.msg_contact_load_failed, Toast.LENGTH_SHORT).show();
    }
}
```

What that function does: I open the contact dialog, or I toast a load failure.
What the function is responsible for: I am the Retrofit success path for getClaimContact.
Where it works: Retrofit calls me from showOwnerContact.
Why I chose to write the code of the function the way its written: isFinishing stops a dialog after the user left.
More: I use ContactInfoDialog.

```java
public void onFailure(@NonNull Call<ContactResponse> call,
                      @NonNull Throwable t) {
    if (isFinishing()) {
        return;
    }
    Toast.makeText(ReportHistoryActivity.this,
            R.string.msg_contact_load_failed, Toast.LENGTH_SHORT).show();
}
```

What that function does: I toast that contact could not load.
What the function is responsible for: I am the Retrofit failure path for getClaimContact.
Where it works: Retrofit calls me from showOwnerContact.
Why I chose to write the code of the function the way its written: A simple failed toast is enough here.
More: I use msg_contact_load_failed.

```java
@Override
protected void onDestroy() {
    super.onDestroy();
    binding = null;
}
```

What that function does: I drop the binding when the screen is destroyed.
What the function is responsible for: I stop onSectionLoaded from touching a dead view.
Where it works: Android calls me. onSectionLoaded checks binding == null.
Why I chose to write the code of the function the way its written: Nulling binding matches the fragment pattern.
More: I set binding to null.
