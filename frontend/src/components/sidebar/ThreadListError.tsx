export function ThreadListError() {
  return (
    <div className="flex items-center justify-center h-32">
      <div role="alert" className="text-sm text-destructive">Failed to load threads</div>
    </div>
  );
}
