export function MessageSkeleton() {
  return (
    <div className="flex w-full gap-3 px-4 py-6" aria-hidden="true">
      <div className="h-10 w-10 flex-shrink-0 rounded-full bg-muted animate-pulse" />
      <div className="flex-1 space-y-2">
        <div className="flex items-center gap-2">
          <div className="h-4 w-24 rounded bg-muted animate-pulse" />
          <div className="h-3 w-16 rounded bg-muted/50 animate-pulse" />
        </div>
        <div className="space-y-1.5">
          <div className="h-4 w-3/4 rounded bg-muted animate-pulse" />
          <div className="h-4 w-1/2 rounded bg-muted animate-pulse" />
          <div className="h-4 w-2/3 rounded bg-muted animate-pulse" />
        </div>
      </div>
    </div>
  );
}
