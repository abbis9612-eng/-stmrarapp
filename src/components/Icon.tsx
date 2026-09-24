type Name = "today" | "eat" | "coach" | "move" | "progress" | "check" | "plus" | "minus" | "send" | "close" | "play" | "pause" | "star" | "trash" | "back";

const paths: Record<Name, string> = {
  today: "M4 11a8 8 0 0 1 16 0v8a1 1 0 0 1-1 1H5a1 1 0 0 1-1-1z M9 20v-5h6v5",
  eat: "M7 3v8 M4 3v5a3 3 0 0 0 6 0V3 M7 11v10 M17 21V3c-2 1-3 4-3 7v3h3",
  coach: "M4 6a2 2 0 0 1 2-2h12a2 2 0 0 1 2 2v9a2 2 0 0 1-2 2h-6l-5 4v-4H6a2 2 0 0 1-2-2z M8.5 10.5h.01 M12 10.5h.01 M15.5 10.5h.01",
  move: "M13 4.5a1.5 1.5 0 1 0 0 .01 M7 21l3-6 3 2v4 M10 15l1-5 4 3 3-1 M6 11l3-2 3 1",
  progress: "M4 19h16 M6 15l4-4 3 3 5-6",
  check: "M5 12.5l4.5 4.5L19 7.5",
  plus: "M12 5v14 M5 12h14",
  minus: "M5 12h14",
  send: "M4 12l16-8-6 16-2.5-6.5z",
  close: "M6 6l12 12 M18 6L6 18",
  play: "M8 5v14l11-7z",
  pause: "M8 5v14 M16 5v14",
  star: "M12 4l2.4 5 5.6.6-4.2 3.8 1.2 5.6L12 16.2 7 19l1.2-5.6L4 9.6 9.6 9z",
  trash: "M5 7h14 M10 11v6 M14 11v6 M6 7l1 13h10l1-13 M9 7V4h6v3",
  back: "M9 6l6 6-6 6",
};

export function Icon({ name, size = 24, filled = false }: { name: Name; size?: number; filled?: boolean }) {
  return (
    <svg width={size} height={size} viewBox="0 0 24 24" fill={filled ? "currentColor" : "none"} stroke="currentColor" strokeWidth={2} strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
      {paths[name].split(" M").map((d, i) => (
        <path key={i} d={i === 0 ? d : `M${d}`} />
      ))}
    </svg>
  );
}
