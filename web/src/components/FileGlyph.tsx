import { HugeiconsIcon } from '@hugeicons/react';
import {
  AndroidIcon,
  Archive01Icon,
  File01Icon,
  File02Icon,
  FileAttachmentIcon,
  FileAudioIcon,
  FileVideoIcon,
  Image01Icon,
} from '@hugeicons/core-free-icons';
import { extensionOf } from '../lib/format';

const AUDIO = new Set(['mp3', 'flac', 'wav', 'm4a', 'aac', 'ogg', 'opus']);
const VIDEO = new Set(['mp4', 'mkv', 'mov', 'webm', 'avi', 'm4v', 'ts']);
const IMAGE = new Set(['jpg', 'jpeg', 'png', 'webp', 'gif', 'avif', 'heic', 'svg']);
const ARCHIVE = new Set(['zip', 'rar', '7z', 'tar', 'gz', 'bz2', 'xz']);
const DOCUMENT = new Set(['pdf', 'doc', 'docx', 'txt', 'md', 'csv', 'xls', 'xlsx', 'ppt', 'pptx']);

function iconFor(extension: string) {
  if (extension === 'apk') return AndroidIcon;
  if (ARCHIVE.has(extension)) return Archive01Icon;
  if (AUDIO.has(extension)) return FileAudioIcon;
  if (VIDEO.has(extension)) return FileVideoIcon;
  if (IMAGE.has(extension)) return Image01Icon;
  if (extension === 'pdf') return FileAttachmentIcon;
  if (DOCUMENT.has(extension)) return File02Icon;
  return File01Icon;
}

interface FileGlyphProps {
  fileName: string;
  active?: boolean;
  className?: string;
}

export function FileGlyph({ fileName, active = false, className = '' }: FileGlyphProps) {
  const extension = extensionOf(fileName);
  const icon = iconFor(extension);

  return (
    <div
      className={`relative grid h-11 w-11 shrink-0 place-items-center rounded-[14px] border ${
        active
          ? 'border-white/15 bg-white/[0.08] text-white'
          : 'border-white/[0.07] bg-white/[0.035] text-white/70'
      } ${className}`}
    >
      <HugeiconsIcon icon={icon} size={20} strokeWidth={1.65} />
      {extension ? (
        <span className="absolute -bottom-1 right-1 rounded bg-[#070708] px-1 text-[8px] font-bold uppercase leading-3 tracking-[0.04em] text-white/35">
          {extension.slice(0, 4)}
        </span>
      ) : null}
    </div>
  );
}
