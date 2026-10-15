# Topic provisioning prerequisites

Provision `financial-events` and `financial-events.DLT` with matching partition counts (e.g. 3), replication factor 3 on a multi-broker cluster, and `min.insync.replicas=2` before rollout. Protect DLT messages and restrict replay permissions. The consumer error handler retries transient exceptions with exponential backoff, then publishes to DLT; DLT publishing must be monitored and periodically replayed with event-ID deduplication.
