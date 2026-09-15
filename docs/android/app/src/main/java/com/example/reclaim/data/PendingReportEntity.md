```java
public Item toItem() {
    Item item = new Item();
    item.setTitle(title);
    item.setDescription(description);
    item.setCategory(category);
    item.setLocation(location);
    item.setType(type);
    item.setVerificationQuestion(verificationQuestion);
    item.setLatitude(latitude);
    item.setLongitude(longitude);
    item.setImageUrl(remoteImageUrl);
    item.setTargetLostReportId(targetLostReportId);
    return item;
}
```

What that function does: I copy this queued row into an Item ready to POST.
What the function is responsible for: I turn Room fields into the API body.
Where it works: I am called by ReportSyncWorker.syncOne.
Why I chose to write the code of the function the way its written: I skip localImagePath so the worker can upload the file first and then set imageUrl.
More: I use Item setters. Public Room fields hold the queued values.
