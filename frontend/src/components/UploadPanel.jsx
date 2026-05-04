import { useState, useRef, useEffect } from 'react'

const STATUS = {
  IDLE: 'idle',
  UPLOADING: 'uploading',
  SUCCESS: 'success',
  ERROR: 'error',
}

export default function UploadPanel({ onDocumentReady, activeDocument, onToast }) {
  const [file, setFile] = useState(null)
  const [status, setStatus] = useState(STATUS.IDLE)
  const [error, setError] = useState(null)
  const [savedDocs, setSavedDocs] = useState([])
  const [deletingId, setDeletingId] = useState(null)
  const inputRef = useRef(null)

  useEffect(() => {
    fetch('/api/documents')
      .then((r) => r.ok ? r.json() : [])
      .then(setSavedDocs)
      .catch(() => {})
  }, [activeDocument])

  function handleFileChange(e) {
    const selected = e.target.files[0]
    if (!selected) return
    if (selected.type !== 'application/pdf') {
      setError('Only PDF files are supported.')
      setFile(null)
      return
    }
    setFile(selected)
    setError(null)
    setStatus(STATUS.IDLE)
  }

  function handleDrop(e) {
    e.preventDefault()
    const dropped = e.dataTransfer.files[0]
    if (!dropped) return
    if (dropped.type !== 'application/pdf') {
      setError('Only PDF files are supported.')
      return
    }
    setFile(dropped)
    setError(null)
    setStatus(STATUS.IDLE)
  }

  async function handleUpload() {
    if (!file) return
    setStatus(STATUS.UPLOADING)
    setError(null)

    const formData = new FormData()
    formData.append('file', file)

    try {
      const res = await fetch('/api/documents', {
        method: 'POST',
        body: formData,
      })

      if (!res.ok) {
        const text = await res.text()
        throw new Error(text || `Server error ${res.status}`)
      }

      const data = await res.json()
      setStatus(STATUS.SUCCESS)
      onDocumentReady({
        id: data.document_id,
        filename: data.filename,
        chunkCount: data.chunks ?? '?',
      })
    } catch (err) {
      setStatus(STATUS.ERROR)
      setError(err.message)
    }
  }

  function handleReset() {
    setFile(null)
    setStatus(STATUS.IDLE)
    setError(null)
    onDocumentReady(null)
    if (inputRef.current) inputRef.current.value = ''
  }

  const isUploading = status === STATUS.UPLOADING

  return (
    <div className="bg-white rounded-xl p-6 shadow-sm flex flex-col gap-4">
      <h2 className="text-base font-semibold text-navy">Upload CV</h2>

      {!activeDocument ? (
        <>
          <div
            className={`border-2 border-dashed rounded-lg py-8 px-4 text-center cursor-pointer transition-colors text-sm
              ${file
                ? 'border-indigo-600 bg-indigo-50 text-indigo-600'
                : 'border-slate-300 text-slate-500 hover:border-indigo-600 hover:bg-indigo-50 hover:text-indigo-600'
              }`}
            onDrop={handleDrop}
            onDragOver={(e) => e.preventDefault()}
            onClick={() => inputRef.current?.click()}
          >
            <input
              ref={inputRef}
              type="file"
              accept="application/pdf"
              onChange={handleFileChange}
              className="hidden"
            />
            {file ? (
              <div className="flex flex-col items-center gap-1.5">
                <span className="text-4xl leading-none">📄</span>
                <span className="font-semibold break-all">{file.name}</span>
                <span className="text-xs text-slate-500">{(file.size / 1024).toFixed(1)} KB</span>
              </div>
            ) : (
              <div className="flex flex-col items-center gap-2">
                <span className="text-4xl leading-none">⬆</span>
                <p>Drop a PDF here or click to browse</p>
              </div>
            )}
          </div>

          {error && (
            <p className="text-red-600 text-sm bg-red-50 border border-red-200 rounded-md px-3 py-2">
              {error}
            </p>
          )}

          <button
            className="w-full py-3 bg-indigo-600 text-white rounded-lg text-[0.9375rem] font-semibold
              flex items-center justify-center min-h-[44px] transition-colors
              hover:bg-indigo-700 disabled:opacity-50 disabled:cursor-not-allowed"
            onClick={handleUpload}
            disabled={!file || isUploading}
          >
            {isUploading ? (
              <span className="w-[18px] h-[18px] border-2 border-white/40 border-t-white rounded-full animate-spin" />
            ) : (
              'Upload & Process'
            )}
          </button>
        </>
      ) : (
        <div className="flex flex-col items-center gap-2 py-6 px-4 bg-green-50 border border-green-200 rounded-lg text-center">
          <div className="w-10 h-10 bg-green-500 text-white rounded-full flex items-center justify-center text-xl font-bold">
            ✓
          </div>
          <p className="font-semibold text-[0.9375rem] break-all">{activeDocument.filename}</p>
          <p className="text-sm text-green-600 font-medium">{activeDocument.chunkCount} chunks indexed</p>
          <p className="text-xs text-slate-500 font-mono">{activeDocument.id}</p>
          <button
            className="mt-2 px-4 py-2 border border-slate-300 rounded-md text-[0.8125rem] text-slate-600 transition-colors hover:bg-slate-100"
            onClick={handleReset}
          >
            Upload a different CV
          </button>
        </div>
      )}
      {savedDocs.length > 0 && (
        <div className="flex flex-col gap-2">
          <p className="text-xs font-semibold text-slate-400 uppercase tracking-wide">Saved CVs</p>
          <ul className="flex flex-col gap-1.5">
            {savedDocs.map((doc) => (
              <li key={doc.document_id} className={`flex items-center gap-1.5 transition-opacity duration-300 ${deletingId === doc.document_id ? 'opacity-0' : 'opacity-100'}`}>
                <button
                  onClick={() => onDocumentReady({ id: doc.document_id, filename: doc.filename, chunkCount: doc.chunks })}
                  className={`flex-1 text-left px-3 py-2 rounded-lg border text-sm transition-colors min-w-0
                    ${activeDocument?.id === doc.document_id
                      ? 'border-indigo-400 bg-indigo-50 text-indigo-700 font-medium'
                      : 'border-slate-200 hover:border-indigo-300 hover:bg-slate-50 text-slate-700'
                    }`}
                >
                  <span className="block truncate">{doc.filename}</span>
                  <span className="text-xs text-slate-400">{doc.chunks} chunks</span>
                </button>
                <button
                  onClick={async () => {
                    setDeletingId(doc.document_id)
                    await new Promise((r) => setTimeout(r, 300))
                    const res = await fetch(`/api/documents/${doc.document_id}`, { method: 'DELETE' })
                    if (!res.ok) {
                      setDeletingId(null)
                      return
                    }
                    if (activeDocument?.id === doc.document_id) onDocumentReady(null)
                    setSavedDocs((prev) => prev.filter((d) => d.document_id !== doc.document_id))
                    setDeletingId(null)
                    onToast?.(`"${doc.filename}" deleted`)
                  }}
                  className="shrink-0 w-7 h-7 flex items-center justify-center rounded-md text-slate-400
                    hover:bg-red-50 hover:text-red-500 transition-colors"
                  title="Delete"
                >
                  ✕
                </button>
              </li>
            ))}
          </ul>
        </div>
      )}
    </div>
  )
}
