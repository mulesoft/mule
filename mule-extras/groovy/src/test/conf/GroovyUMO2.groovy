class testComponent2 {
    onCall(event) {
        return event.getTransformedMessageAsString() + " Received by component 2:";
    }
}