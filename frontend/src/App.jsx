import { useState, useEffect } from 'react'
import UploadPanel from './components/UploadPanel'
import ChatPanel from './components/ChatPanel'

const LS_KEY = 'cv-assistant:document'

export default function App() {
  const [document, setDocument] = useState(null) // { id, filename, chunkCount }
  const [toast, setToast] = useState(null)
  const [toastVisible, setToastVisible] = useState(false)

  function showToast(message) {
    setToast(message)
    setToastVisible(true)
    setTimeout(() => setToastVisible(false), 2500)
    setTimeout(() => setToast(null), 3000)
  }

  // On mount, restore the last active document from localStorage if backend still has it
  useEffect(() => {
    const saved = localStorage.getItem(LS_KEY)
    if (!saved) return
    let parsed
    try {
      parsed = JSON.parse(saved)
    } catch {
      localStorage.removeItem(LS_KEY)
      return
    }
    fetch(`/api/documents/${parsed.id}`)
      .then((r) => r.ok ? r.json() : Promise.reject())
      .then((data) => setDocument({ id: data.document_id, filename: data.filename, chunkCount: data.chunks }))
      .catch(() => localStorage.removeItem(LS_KEY))
  }, [])

  function handleDocumentReady(doc) {
    setDocument(doc)
    if (doc) localStorage.setItem(LS_KEY, JSON.stringify(doc))
    else localStorage.removeItem(LS_KEY)
  }

  return (
    <div className="min-h-screen flex flex-col">
      {toast && (
        <div className={`fixed bottom-6 left-1/2 -translate-x-1/2 z-50 bg-slate-800 text-white text-sm px-4 py-2.5 rounded-lg shadow-lg
          transition-all duration-300 ${toastVisible ? 'opacity-100 translate-y-0' : 'opacity-0 translate-y-2'}`}>
          {toast}
        </div>
      )}
      <header className="bg-navy text-white px-8 py-5 border-b-[3px] border-indigo-600 shrink-0">
        <h1 className="text-2xl font-bold tracking-tight">CV Assistant</h1>
        <p className="text-sm text-slate-400 mt-1">Upload a CV and ask questions about it</p>
      </header>

      <main className="flex-1 p-6 min-h-0">
        <div className="grid grid-cols-[340px_1fr] gap-6 max-w-[1200px] mx-auto h-[calc(100vh-120px)] max-[900px]:grid-cols-1 max-[900px]:h-auto">
          <section className="flex flex-col min-w-0">
            <UploadPanel onDocumentReady={handleDocumentReady} activeDocument={document} onToast={showToast} />
          </section>
          <section className="flex flex-col min-h-0">
            <ChatPanel document={document} />
          </section>
        </div>
      </main>
    </div>
  )
}
