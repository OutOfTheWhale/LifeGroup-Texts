import { useState } from "react";

const formatPhone = (raw) => {
  const digits = raw.replace(/\D/g, "");
  if (digits.length === 10) return `(${digits.slice(0,3)}) ${digits.slice(3,6)}-${digits.slice(6)}`;
  if (digits.length === 11 && digits[0] === "1") return `+1 (${digits.slice(1,4)}) ${digits.slice(4,7)}-${digits.slice(7)}`;
  return raw;
};

const SMS_LIMIT = 160;

const STORAGE_KEYS = { contacts:"lgt_contacts", groups:"lgt_groups", messages:"lgt_messages", sentLog:"lgt_sentlog" };
const defaultContacts = [
  { id:1, name:"Mary Johnson", phone:"3615550101" },
  { id:2, name:"Pastor Dave", phone:"3615550102" },
  { id:3, name:"Sarah & Tom", phone:"3615550103" },
];
const defaultGroups = [{ id:1, name:"Sunday Life Group", contactIds:[1,2,3] }];
const defaultMessages = ["Hi {name}, just a reminder about Sunday service at 10am. God bless! 🙏"];
const load = (key, fallback) => { try { const v = localStorage.getItem(key); return v ? JSON.parse(v) : fallback; } catch { return fallback; } };
const save = (key, val) => { try { localStorage.setItem(key, JSON.stringify(val)); } catch {} };

const inputStyle = { width:"100%", padding:"11px 13px", borderRadius:10, border:"1px solid #e8d9c5", fontSize:14, background:"#fdf6ec", boxSizing:"border-box", outline:"none", color:"#2d3748", fontFamily:"Georgia, serif" };
const btnStyle = { background:"linear-gradient(135deg, #4a7c59, #2d5a3d)", color:"#fff", border:"none", borderRadius:10, padding:"12px", fontSize:14, fontWeight:"bold", cursor:"pointer" };
const btnStyleOutline = { background:"#fff", color:"#4a7c59", border:"1px solid #4a7c59", borderRadius:10, padding:"12px", fontSize:14, cursor:"pointer" };

async function sendAllNative(phone, messageList) {
  try {
    if (window.Capacitor && window.Capacitor.Plugins && window.Capacitor.Plugins.SmsManager) {
      await window.Capacitor.Plugins.SmsManager.sendAll({ to: phone, messages: messageList });
      return true;
    }
    window.location.href = `sms:${phone}?body=${encodeURIComponent(message)}`;
    await new Promise(r => setTimeout(r, 2000));
    return false;
  } catch(e) {
    window.location.href = `sms:${phone}?body=${encodeURIComponent(message)}`;
    await new Promise(r => setTimeout(r, 2000));    return false;
  }
}

async function getPhoneContacts() {
  try {
    if (window.Capacitor && window.Capacitor.Plugins && window.Capacitor.Plugins.ContactsPlugin) {
      const result = await window.Capacitor.Plugins.ContactsPlugin.getContacts();
      return result.contacts || [];
    }
    return [];
  } catch(e) { return []; }
}

function CharCounter({ text }) {
  const len = text.length;
  const over = len > SMS_LIMIT;
  const color = over ? "#c0392b" : len > 130 ? "#e67e22" : "#aaa";
  return (
    <div style={{ display:"flex", justifyContent:"space-between", alignItems:"center", marginTop:4 }}>
      <div style={{ fontSize:11, color:over?"#c0392b":"transparent", fontWeight:"bold" }}>
        {over ? `⚠️ Over 160 — will send as ${Math.ceil(len/SMS_LIMIT)} texts` : " "}
      </div>
      <div style={{ fontSize:12, color, fontWeight: over?"bold":"normal" }}>{len}/160</div>
    </div>
  );
}

export default function App() {
  const [contacts, setContactsRaw] = useState(() => load(STORAGE_KEYS.contacts, defaultContacts));
  const [groups, setGroupsRaw] = useState(() => load(STORAGE_KEYS.groups, defaultGroups));
  const [messages, setMessagesRaw] = useState(() => load(STORAGE_KEYS.messages, defaultMessages));
  const [sentLog, setSentLogRaw] = useState(() => load(STORAGE_KEYS.sentLog, []));
  const [view, setView] = useState("home");
  const [editingContact, setEditingContact] = useState(null);
  const [newName, setNewName] = useState("");
  const [newPhone, setNewPhone] = useState("");
  const [sending, setSending] = useState(false);
  const [sendProgress, setSendProgress] = useState({ current:0, total:0 });
  const [showAddForm, setShowAddForm] = useState(false);
  const [toast, setToast] = useState(null);
  const [expandedGroups, setExpandedGroups] = useState({});
  const [showAddGroup, setShowAddGroup] = useState(false);
  const [newGroupName, setNewGroupName] = useState("");
  const [editingGroup, setEditingGroup] = useState(null);
  const [selectedContactIds, setSelectedContactIds] = useState(new Set());
  const [selectedGroupIds, setSelectedGroupIds] = useState(new Set());
  const [showPhonePicker, setShowPhonePicker] = useState(false);
  const [phoneContacts, setPhoneContacts] = useState([]);
  const [pickerSearch, setPickerSearch] = useState("");
  const [loadingPhoneContacts, setLoadingPhoneContacts] = useState(false);

  const setContacts = (v) => { setContactsRaw(v); save(STORAGE_KEYS.contacts, v); };
  const setGroups = (v) => { setGroupsRaw(v); save(STORAGE_KEYS.groups, v); };
  const setMessages = (v) => { setMessagesRaw(v); save(STORAGE_KEYS.messages, v); };
  const setSentLog = (v) => { setSentLogRaw(v); save(STORAGE_KEYS.sentLog, v); };

  const showToast = (msg, type="success") => { setToast({ msg, type }); setTimeout(() => setToast(null), 3500); };

  // Message box handlers
  const updateMessage = (idx, val) => {
    const updated = messages.map((m, i) => i === idx ? val : m);
    setMessages(updated);
  };
  const addMessageBox = () => setMessages([...messages, ""]);
  const removeMessageBox = (idx) => {
    if (messages.length === 1) return;
    setMessages(messages.filter((_, i) => i !== idx));
  };

  const openPhonePicker = async () => {
    setShowPhonePicker(true); setLoadingPhoneContacts(true); setPickerSearch("");
    const result = await getPhoneContacts();
    setPhoneContacts(result); setLoadingPhoneContacts(false);
  };
  const addFromPhone = (pc) => {
    if (contacts.find(c => c.phone === pc.phone)) { showToast(`${pc.name} already added`, "info"); return; }
    setContacts([...contacts, { id:Date.now(), name:pc.name, phone:pc.phone }]);
    showToast(`${pc.name} added!`);
  };
  const filteredPhoneContacts = phoneContacts.filter(c =>
    c.name.toLowerCase().includes(pickerSearch.toLowerCase()) || c.phone.includes(pickerSearch)
  );

  const addContact = () => {
    if (!newName.trim() || !newPhone.trim()) return;
    setContacts([...contacts, { id:Date.now(), name:newName.trim(), phone:newPhone.replace(/\D/g,"") }]);
    setNewName(""); setNewPhone(""); setShowAddForm(false); showToast("Contact added!");
  };
  const deleteContact = (id) => {
    setContacts(contacts.filter(c => c.id !== id));
    setGroups(groups.map(g => ({ ...g, contactIds:g.contactIds.filter(cid => cid !== id) })));
    setSelectedContactIds(prev => { const s = new Set(prev); s.delete(id); return s; });
    showToast("Contact removed", "info");
  };
  const saveEdit = () => {
    setContacts(contacts.map(c => c.id === editingContact.id ? { ...editingContact, phone:editingContact.phone.replace(/\D/g,"") } : c));
    setEditingContact(null); showToast("Contact updated!");
  };
  const addGroup = () => {
    if (!newGroupName.trim()) return;
    setGroups([...groups, { id:Date.now(), name:newGroupName.trim(), contactIds:[] }]);
    setNewGroupName(""); setShowAddGroup(false); showToast("Group created!");
  };
  const deleteGroup = (id) => {
    setGroups(groups.filter(g => g.id !== id));
    setSelectedGroupIds(prev => { const s = new Set(prev); s.delete(id); return s; });
    showToast("Group removed", "info");
  };
  const toggleContactInGroup = (groupId, contactId) => {
    setGroups(groups.map(g => {
      if (g.id !== groupId) return g;
      const has = g.contactIds.includes(contactId);
      return { ...g, contactIds: has ? g.contactIds.filter(id => id !== contactId) : [...g.contactIds, contactId] };
    }));
  };
  const toggleGroupExpand = (id) => setExpandedGroups(prev => ({ ...prev, [id]:!prev[id] }));
  const toggleContactSelect = (id) => { setSelectedContactIds(prev => { const s = new Set(prev); s.has(id)?s.delete(id):s.add(id); return s; }); };
  const toggleGroupSelect = (gid) => { setSelectedGroupIds(prev => { const s = new Set(prev); s.has(gid)?s.delete(gid):s.add(gid); return s; }); };

  const getRecipients = () => {
    const ids = new Set(selectedContactIds);
    selectedGroupIds.forEach(gid => { const g = groups.find(x => x.id===gid); if(g) g.contactIds.forEach(id => ids.add(id)); });
    if (ids.size === 0) contacts.forEach(c => ids.add(c.id));
    return contacts.filter(c => ids.has(c.id));
  };
  const recipients = getRecipients();
  const nothingSelected = selectedContactIds.size === 0 && selectedGroupIds.size === 0;

  const sendMessages = async () => {
    setSending(true);
    const timestamp = new Date().toLocaleString();
    const list = recipients;
    const totalOps = list.length * messages.length;
    setSendProgress({ current:0, total:totalOps });
    let op = 0;

    for (let i = 0; i < list.length; i++) {
      const contact = list[i];
      for (let msgIdx = 0; msgIdx < messages.length; msgIdx++) {
        const personalizedMsg = messages[msgIdx].replace("{name}", contact.name.split(" ")[0]);
        op++;
        setSendProgress({ current:op, total:totalOps });
        try {
          if (window.Capacitor && window.Capacitor.Plugins && window.Capacitor.Plugins.SmsManager) {
            await window.Capacitor.Plugins.SmsManager.send({ to: contact.phone, text: personalizedMsg });
          } else {
            window.location.href = `sms:${contact.phone}?body=${encodeURIComponent(personalizedMsg)}`;
          }
        } catch(e) {}
        await new Promise(r => setTimeout(r, 2000));
      }
      // Extra pause between contacts
      if (i < list.length - 1) await new Promise(r => setTimeout(r, 5000));
    }
    const log = list.map(c => ({
      name:c.name, phone:c.phone,
      msgs: messages.map(m => m.replace("{name}", c.name.split(" ")[0])),
      time:timestamp,
    }));
    setSentLog([...log, ...sentLog]);
    setSending(false); setSendProgress({ current:0, total:0 });
    setView("sent");
    showToast(`Sent ${messages.length > 1 ? messages.length+" texts" : "message"} to ${list.length} contact${list.length!==1?"s":""}! 🎉`);
  };

  const firstMsg = messages[0] || "";

  return (
    <div style={{ fontFamily:"'Georgia', serif", background:"linear-gradient(160deg, #fdf6ec 0%, #f5ede0 100%)", minHeight:"100vh", maxWidth:430, margin:"0 auto", position:"relative" }}>

      {toast && <div style={{ position:"fixed", top:16, left:"50%", transform:"translateX(-50%)", background:toast.type==="success"?"#4a7c59":"#8a6f4e", color:"#fff", padding:"10px 20px", borderRadius:24, fontSize:14, zIndex:999, boxShadow:"0 4px 16px rgba(0,0,0,0.2)", animation:"fadeIn 0.3s ease", whiteSpace:"nowrap" }}>{toast.msg}</div>}

      {sending && (
        <div style={{ position:"fixed", top:0, left:0, right:0, bottom:0, background:"rgba(0,0,0,0.6)", zIndex:998, display:"flex", alignItems:"center", justifyContent:"center" }}>
          <div style={{ background:"#fff", borderRadius:20, padding:"32px 40px", textAlign:"center", boxShadow:"0 8px 32px rgba(0,0,0,0.3)" }}>
            <div style={{ fontSize:40, marginBottom:12 }}>📨</div>
            <div style={{ fontSize:18, fontWeight:"bold", color:"#2d5a3d", marginBottom:8 }}>Sending Messages</div>
            <div style={{ fontSize:14, color:"#8a6f4e", marginBottom:16 }}>{sendProgress.current} of {sendProgress.total} sent</div>
            <div style={{ background:"#f0e6d6", borderRadius:10, height:8, width:200, overflow:"hidden" }}>
              <div style={{ background:"#4a7c59", height:"100%", borderRadius:10, width:`${sendProgress.total>0?(sendProgress.current/sendProgress.total)*100:0}%`, transition:"width 0.4s ease" }} />
            </div>
          </div>
        </div>
      )}

      {showPhonePicker && (
        <div style={{ position:"fixed", top:0, left:0, right:0, bottom:0, background:"rgba(0,0,0,0.6)", zIndex:997, display:"flex", alignItems:"flex-end" }}>
          <div style={{ background:"#fff", borderRadius:"20px 20px 0 0", width:"100%", maxHeight:"80vh", display:"flex", flexDirection:"column" }}>
            <div style={{ padding:"16px 16px 8px", borderBottom:"1px solid #e8d9c5" }}>
              <div style={{ display:"flex", justifyContent:"space-between", alignItems:"center", marginBottom:12 }}>
                <div style={{ fontSize:17, fontWeight:"bold", color:"#2d5a3d" }}>📱 Import from Phone</div>
                <button onClick={() => setShowPhonePicker(false)} style={{ background:"none", border:"none", fontSize:22, cursor:"pointer", color:"#999" }}>✕</button>
              </div>
              <input value={pickerSearch} onChange={e => setPickerSearch(e.target.value)} placeholder="🔍 Search contacts..." style={{...inputStyle, fontSize:15}} autoFocus />
            </div>
            <div style={{ overflowY:"auto", flex:1, padding:"8px 12px 24px" }}>
              {loadingPhoneContacts ? (
                <div style={{ textAlign:"center", padding:40, color:"#8a6f4e", fontSize:15 }}>Loading contacts...</div>
              ) : filteredPhoneContacts.length === 0 ? (
                <div style={{ textAlign:"center", padding:40, color:"#aaa", fontSize:14 }}>
                  {pickerSearch ? "No contacts match your search" : "No contacts found on device"}
                </div>
              ) : filteredPhoneContacts.map((c, i) => {
                const alreadyAdded = contacts.some(e => e.phone === c.phone);
                return (
                  <div key={i} style={{ display:"flex", alignItems:"center", padding:"10px 8px", borderBottom:"1px solid #f5ede0" }}>
                    <div style={{ width:36, height:36, borderRadius:"50%", background:"linear-gradient(135deg, #4a7c59, #8ab89a)", display:"flex", alignItems:"center", justifyContent:"center", color:"#fff", fontWeight:"bold", fontSize:14, marginRight:12, flexShrink:0 }}>{c.name[0]}</div>
                    <div style={{ flex:1 }}>
                      <div style={{ fontWeight:"bold", color:"#2d3748", fontSize:14 }}>{c.name}</div>
                      <div style={{ fontSize:12, color:"#8a6f4e", fontFamily:"monospace" }}>{formatPhone(c.phone)}</div>
                    </div>
                    <button onClick={() => addFromPhone(c)} disabled={alreadyAdded} style={{ padding:"6px 14px", borderRadius:20, fontSize:12, fontWeight:"bold", cursor:alreadyAdded?"default":"pointer", background:alreadyAdded?"#f0f0f0":"#4a7c59", color:alreadyAdded?"#aaa":"#fff", border:"none" }}>
                      {alreadyAdded ? "Added" : "+ Add"}
                    </button>
                  </div>
                );
              })}
            </div>
          </div>
        </div>
      )}

      <div style={{ background:"linear-gradient(135deg, #4a7c59 0%, #2d5a3d 100%)", padding:"14px 20px", color:"#fff", display:"flex", alignItems:"center", gap:10 }}>
        <span style={{ fontSize:24 }}>📖</span>
        <span style={{ fontSize:19, fontWeight:"bold", letterSpacing:0.3 }}>Life Group Texts</span>
      </div>

      <div style={{ display:"flex", background:"#fff", borderBottom:"1px solid #e8d9c5", position:"sticky", top:0, zIndex:10 }}>
        {[{id:"home",label:"🏠 Home"},{id:"contacts",label:"👥 People"},{id:"compose",label:"✉️ Message"},{id:"sent",label:"📋 Log"}].map(tab => (
          <button key={tab.id} onClick={() => setView(tab.id)} style={{ flex:1, padding:"12px 4px", border:"none", background:view===tab.id?"#fdf6ec":"#fff", borderBottom:view===tab.id?"2px solid #4a7c59":"2px solid transparent", color:view===tab.id?"#4a7c59":"#999", fontSize:11, fontWeight:view===tab.id?"bold":"normal", cursor:"pointer" }}>{tab.label}</button>
        ))}
      </div>

      <div style={{ padding:"18px 16px", paddingBottom:48 }}>

        {view === "home" && (
          <div>
            <div style={{ background:"#fff", borderRadius:16, padding:16, boxShadow:"0 2px 12px rgba(0,0,0,0.06)", marginBottom:14 }}>
              <div style={{ fontSize:13, color:"#8a6f4e", marginBottom:10, fontWeight:"bold" }}>📤 Who to send to</div>
              <div style={{ fontSize:12, color:"#aaa", marginBottom:8 }}>{nothingSelected?"Nothing selected — will send to ALL contacts":`${recipients.length} recipient${recipients.length!==1?"s":""} selected`}</div>
              {groups.length > 0 && (
                <div style={{ marginBottom:8 }}>
                  <div style={{ fontSize:11, color:"#8a6f4e", marginBottom:6, textTransform:"uppercase", letterSpacing:0.5 }}>Groups</div>
                  <div style={{ display:"flex", flexWrap:"wrap", gap:6 }}>
                    {groups.map(g => <button key={g.id} onClick={() => toggleGroupSelect(g.id)} style={{ padding:"6px 12px", borderRadius:20, fontSize:12, border:selectedGroupIds.has(g.id)?"2px solid #4a7c59":"1px solid #ddd", background:selectedGroupIds.has(g.id)?"#e8f5ec":"#fff", color:selectedGroupIds.has(g.id)?"#2d5a3d":"#666", cursor:"pointer", fontWeight:selectedGroupIds.has(g.id)?"bold":"normal" }}>{selectedGroupIds.has(g.id)?"✓ ":""}{g.name} ({g.contactIds.length})</button>)}
                  </div>
                </div>
              )}
              <div>
                <div style={{ fontSize:11, color:"#8a6f4e", marginBottom:6, textTransform:"uppercase", letterSpacing:0.5 }}>Individuals</div>
                <div style={{ display:"flex", flexWrap:"wrap", gap:6 }}>
                  {contacts.map(c => <button key={c.id} onClick={() => toggleContactSelect(c.id)} style={{ padding:"6px 12px", borderRadius:20, fontSize:12, border:selectedContactIds.has(c.id)?"2px solid #4a7c59":"1px solid #ddd", background:selectedContactIds.has(c.id)?"#e8f5ec":"#fff", color:selectedContactIds.has(c.id)?"#2d5a3d":"#666", cursor:"pointer", fontWeight:selectedContactIds.has(c.id)?"bold":"normal" }}>{selectedContactIds.has(c.id)?"✓ ":""}{c.name.split(" ")[0]}</button>)}
                </div>
              </div>
              {!nothingSelected && <button onClick={() => { setSelectedContactIds(new Set()); setSelectedGroupIds(new Set()); }} style={{ marginTop:10, fontSize:12, color:"#c0392b", background:"none", border:"none", cursor:"pointer", padding:0 }}>✕ Clear selection</button>}
            </div>

            <div style={{ background:"#fff", borderRadius:16, padding:16, boxShadow:"0 2px 12px rgba(0,0,0,0.06)", marginBottom:14 }}>
              <div style={{ fontSize:13, color:"#8a6f4e", marginBottom:8 }}>
                Message preview
                {messages.length > 1 && <span style={{ marginLeft:8, background:"#4a7c59", color:"#fff", borderRadius:10, padding:"2px 8px", fontSize:11 }}>{messages.length} texts</span>}
              </div>
              {messages.map((msg, i) => (
                <div key={i} style={{ background:"#f5ede0", borderRadius:10, padding:10, fontSize:13, color:"#3d2b1f", lineHeight:1.5, fontStyle:"italic", marginBottom: i < messages.length-1 ? 6 : 0 }}>
                  {messages.length > 1 && <div style={{ fontSize:10, color:"#8a6f4e", marginBottom:3, fontStyle:"normal" }}>Text {i+1}</div>}
                  "{msg.replace("{name}", recipients[0]?.name.split(" ")[0] || "Friend")}"
                </div>
              ))}
            </div>

            <button onClick={sendMessages} disabled={sending||nothingSelected||recipients.length===0} style={{ width:"100%", padding:"15px", background:(sending||nothingSelected)?"#999":"linear-gradient(135deg, #4a7c59, #2d5a3d)", color:"#fff", border:"none", borderRadius:14, fontSize:16, fontWeight:"bold", cursor:(sending||nothingSelected)?"not-allowed":"pointer", boxShadow:(sending||nothingSelected)?"none":"0 4px 16px rgba(74,124,89,0.4)" }}>
              {sending?"⏳ Sending...":nothingSelected?"👥 Please Select Recipients":`🚀 Send${messages.length>1?` ${messages.length} texts`:""} to ${recipients.length} Contact${recipients.length!==1?"s":""}`}
            </button>
          </div>
        )}

        {view === "contacts" && (
          <div>
            <div style={{ marginBottom:20 }}>
              <div style={{ display:"flex", justifyContent:"space-between", alignItems:"center", marginBottom:10 }}>
                <div style={{ fontSize:16, fontWeight:"bold", color:"#2d5a3d" }}>📁 Groups</div>
                <button onClick={() => setShowAddGroup(!showAddGroup)} style={{ background:"#4a7c59", color:"#fff", border:"none", borderRadius:20, padding:"7px 14px", fontSize:12, cursor:"pointer", fontWeight:"bold" }}>+ New Group</button>
              </div>
              {showAddGroup && (
                <div style={{ background:"#fff", borderRadius:14, padding:14, marginBottom:12, border:"1px solid #e8d9c5" }}>
                  <input value={newGroupName} onChange={e => setNewGroupName(e.target.value)} placeholder="Group name" style={inputStyle} />
                  <div style={{ display:"flex", gap:8, marginTop:8 }}>
                    <button onClick={addGroup} style={{...btnStyle,flex:1,padding:"10px"}}>Create</button>
                    <button onClick={() => setShowAddGroup(false)} style={{...btnStyleOutline,flex:1,padding:"10px"}}>Cancel</button>
                  </div>
                </div>
              )}
              {groups.map(group => (
                <div key={group.id} style={{ background:"#fff", borderRadius:14, marginBottom:10, boxShadow:"0 2px 8px rgba(0,0,0,0.05)", overflow:"hidden" }}>
                  <div style={{ display:"flex", alignItems:"center", padding:"13px 14px", cursor:"pointer", borderBottom:expandedGroups[group.id]?"1px solid #f0e6d6":"none" }} onClick={() => toggleGroupExpand(group.id)}>
                    <span style={{ fontSize:18, marginRight:10 }}>{expandedGroups[group.id]?"📂":"📁"}</span>
                    <div style={{ flex:1 }}>
                      {editingGroup?.id===group.id
                        ? <input value={editingGroup.name} onChange={e => setEditingGroup({...editingGroup,name:e.target.value})} onClick={e => e.stopPropagation()} onKeyDown={e => { if(e.key==="Enter"){setGroups(groups.map(g=>g.id===editingGroup.id?{...g,name:editingGroup.name}:g));setEditingGroup(null);showToast("Group renamed!");}}} style={{...inputStyle,padding:"4px 8px",fontSize:14}} autoFocus />
                        : <><div style={{ fontWeight:"bold", color:"#2d3748", fontSize:15 }}>{group.name}</div><div style={{ fontSize:12, color:"#8a6f4e" }}>{group.contactIds.length} member{group.contactIds.length!==1?"s":""}</div></>
                      }
                    </div>
                    <button onClick={e => { e.stopPropagation(); setEditingGroup(editingGroup?.id===group.id?null:{...group}); }} style={{ background:"none", border:"none", fontSize:16, cursor:"pointer", padding:"0 4px" }}>✏️</button>
                    <button onClick={e => { e.stopPropagation(); deleteGroup(group.id); }} style={{ background:"none", border:"none", fontSize:16, cursor:"pointer", padding:"0 4px" }}>🗑️</button>
                  </div>
                  {expandedGroups[group.id] && (
                    <div style={{ padding:"8px 12px 12px" }}>
                      <div style={{ fontSize:11, color:"#aaa", marginBottom:8, textTransform:"uppercase", letterSpacing:0.5 }}>Tap to add/remove members</div>
                      {contacts.map(c => {
                        const inGroup = group.contactIds.includes(c.id);
                        return <div key={c.id} onClick={() => toggleContactInGroup(group.id, c.id)} style={{ display:"flex", alignItems:"center", padding:"8px 10px", borderRadius:10, marginBottom:4, cursor:"pointer", background:inGroup?"#e8f5ec":"#fafafa", border:inGroup?"1px solid #b2d8bc":"1px solid #eee" }}>
                          <div style={{ width:28, height:28, borderRadius:"50%", background:inGroup?"#4a7c59":"#ccc", display:"flex", alignItems:"center", justifyContent:"center", color:"#fff", fontSize:12, fontWeight:"bold", marginRight:10, flexShrink:0 }}>{inGroup?"✓":c.name[0]}</div>
                          <div style={{ flex:1 }}>
                            <div style={{ fontSize:14, fontWeight:inGroup?"bold":"normal", color:inGroup?"#2d5a3d":"#555" }}>{c.name}</div>
                            <div style={{ fontSize:12, color:"#aaa" }}>{formatPhone(c.phone)}</div>
                          </div>
                        </div>;
                      })}
                    </div>
                  )}
                </div>
              ))}
              {groups.length===0 && <div style={{ textAlign:"center", color:"#bbb", padding:"16px 0", fontSize:14 }}>No groups yet.</div>}
            </div>

            <div>
              <div style={{ display:"flex", justifyContent:"space-between", alignItems:"center", marginBottom:10 }}>
                <div style={{ fontSize:16, fontWeight:"bold", color:"#2d5a3d" }}>👤 All Contacts ({contacts.length})</div>
                <div style={{ display:"flex", gap:6 }}>
                  <button onClick={openPhonePicker} style={{ background:"#e8f5ec", color:"#2d5a3d", border:"1px solid #b2d8bc", borderRadius:20, padding:"7px 12px", fontSize:12, cursor:"pointer", fontWeight:"bold" }}>📱 Import</button>
                  <button onClick={() => setShowAddForm(!showAddForm)} style={{ background:"#4a7c59", color:"#fff", border:"none", borderRadius:20, padding:"7px 14px", fontSize:12, cursor:"pointer", fontWeight:"bold" }}>+ Add</button>
                </div>
              </div>
              {showAddForm && (
                <div style={{ background:"#fff", borderRadius:14, padding:14, marginBottom:12, border:"1px solid #e8d9c5" }}>
                  <input value={newName} onChange={e => setNewName(e.target.value)} placeholder="Full name" style={inputStyle} />
                  <input value={newPhone} onChange={e => setNewPhone(e.target.value)} placeholder="Phone with area code (e.g. 3615550101)" type="tel" style={{...inputStyle,marginTop:8}} />
                  <div style={{ display:"flex", gap:8, marginTop:8 }}>
                    <button onClick={addContact} style={{...btnStyle,flex:1,padding:"10px"}}>Save</button>
                    <button onClick={() => setShowAddForm(false)} style={{...btnStyleOutline,flex:1,padding:"10px"}}>Cancel</button>
                  </div>
                </div>
              )}
              {contacts.map(contact => (
                <div key={contact.id}>
                  {editingContact?.id===contact.id
                    ? <div style={{ background:"#fff", borderRadius:14, padding:14, marginBottom:10, border:"2px solid #4a7c59" }}>
                        <input value={editingContact.name} onChange={e => setEditingContact({...editingContact,name:e.target.value})} style={inputStyle} />
                        <input value={editingContact.phone} onChange={e => setEditingContact({...editingContact,phone:e.target.value})} style={{...inputStyle,marginTop:8}} type="tel" />
                        <div style={{ display:"flex", gap:8, marginTop:8 }}>
                          <button onClick={saveEdit} style={{...btnStyle,flex:1,padding:"10px"}}>Save</button>
                          <button onClick={() => setEditingContact(null)} style={{...btnStyleOutline,flex:1,padding:"10px"}}>Cancel</button>
                        </div>
                      </div>
                    : <div style={{ background:"#fff", borderRadius:14, padding:"12px 14px", marginBottom:8, display:"flex", alignItems:"center", boxShadow:"0 2px 8px rgba(0,0,0,0.04)" }}>
                        <div style={{ width:38, height:38, borderRadius:"50%", background:"linear-gradient(135deg, #4a7c59, #8ab89a)", display:"flex", alignItems:"center", justifyContent:"center", color:"#fff", fontWeight:"bold", fontSize:15, marginRight:12, flexShrink:0 }}>{contact.name[0]}</div>
                        <div style={{ flex:1 }}>
                          <div style={{ fontWeight:"bold", color:"#2d3748", fontSize:14 }}>{contact.name}</div>
                          <div style={{ color:"#8a6f4e", fontSize:13, fontFamily:"monospace" }}>{formatPhone(contact.phone)}</div>
                        </div>
                        <button onClick={() => setEditingContact({...contact})} style={{ background:"none", border:"none", fontSize:17, cursor:"pointer", padding:4 }}>✏️</button>
                        <button onClick={() => deleteContact(contact.id)} style={{ background:"none", border:"none", fontSize:17, cursor:"pointer", padding:4 }}>🗑️</button>
                      </div>
                  }
                </div>
              ))}
              {contacts.length===0 && <div style={{ textAlign:"center", color:"#aaa", padding:32, fontSize:14 }}>No contacts yet.</div>}
            </div>
          </div>
        )}

        {view === "compose" && (
          <div>
            <div style={{ fontSize:16, fontWeight:"bold", color:"#2d5a3d", marginBottom:4 }}>Your Messages</div>
            <div style={{ fontSize:13, color:"#8a6f4e", marginBottom:14 }}>
              Use <strong>{"{name}"}</strong> to personalize. A new text box appears automatically at 160 characters.
            </div>

            {messages.map((msg, idx) => (
              <div key={idx} style={{ background:"#fff", borderRadius:14, padding:14, marginBottom:10, border:`1px solid ${msg.length > SMS_LIMIT ? "#c0392b" : "#e8d9c5"}`, boxShadow:"0 2px 8px rgba(0,0,0,0.04)" }}>
                <div style={{ display:"flex", justifyContent:"space-between", alignItems:"center", marginBottom:8 }}>
                  <div style={{ fontSize:13, fontWeight:"bold", color:"#2d5a3d" }}>
                    Text {idx+1} {messages.length > 1 ? `of ${messages.length}` : ""}
                  </div>
                  {messages.length > 1 && (
                    <button onClick={() => removeMessageBox(idx)} style={{ background:"none", border:"none", color:"#c0392b", fontSize:13, cursor:"pointer", padding:0 }}>✕ Remove</button>
                  )}
                </div>
                <textarea
                  value={msg}
                  onChange={e => {
                    updateMessage(idx, e.target.value);
                    // Auto-add new box when hitting limit on the last box
                    if (e.target.value.length >= SMS_LIMIT && idx === messages.length - 1 && messages.length < 2) {
                      addMessageBox();
                    }
                  }}
                  rows={4}
                  style={{...inputStyle, resize:"none", lineHeight:1.6, fontSize:14, fontFamily:"Georgia, serif",
                    borderColor: msg.length > SMS_LIMIT ? "#c0392b" : "#e8d9c5"
                  }}
                />
                <CharCounter text={msg} />
              </div>
            ))}

            {true && (
              <button onClick={addMessageBox} style={{ width:"100%", padding:"11px", background:"#fff", color:"#4a7c59", border:"2px dashed #4a7c59", borderRadius:14, fontSize:13, fontWeight:"bold", cursor:"pointer", marginBottom:14 }}>
                + Add Another Text
              </button>
            )}

            {/* Single preview at the bottom showing all texts in sequence */}
            <div style={{ background:"#fff", borderRadius:14, padding:14, border:"1px solid #e8d9c5", marginBottom:14, boxShadow:"0 2px 8px rgba(0,0,0,0.04)" }}>
              <div style={{ fontSize:13, color:"#8a6f4e", marginBottom:10, fontWeight:"bold" }}>
                📱 Preview — as {contacts[0]?.name.split(" ")[0] || "Friend"} sees it
              </div>
              <div style={{ display:"flex", flexDirection:"column", gap:6 }}>
                {messages.map((msg, idx) => (
                  <div key={idx}>
                    {messages.length > 1 && (
                      <div style={{ fontSize:10, color:"#aaa", marginBottom:3, textAlign:"right" }}>Text {idx+1}</div>
                    )}
                    <div style={{ background:"#dcf8c6", borderRadius:"12px 12px 4px 12px", padding:"10px 14px", fontSize:13, color:"#2d3748", maxWidth:"85%", marginLeft:"auto", lineHeight:1.5 }}>
                      {msg.replace("{name}", contacts[0]?.name.split(" ")[0] || "Friend")}
                    </div>
                  </div>
                ))}
              </div>
            </div>

            <button onClick={() => setView("home")} style={{...btnStyle, width:"100%"}}>
              ✅ Save & Go to Send
            </button>
          </div>
        )}

        {view === "sent" && (
          <div>
            <div style={{ fontSize:16, fontWeight:"bold", color:"#2d5a3d", marginBottom:14 }}>Send Log ({sentLog.length})</div>
            {sentLog.length===0
              ? <div style={{ textAlign:"center", color:"#aaa", padding:40, fontSize:14 }}>No messages sent yet.</div>
              : sentLog.map((entry,i) => (
                <div key={i} style={{ background:"#fff", borderRadius:12, padding:14, marginBottom:10, boxShadow:"0 2px 8px rgba(0,0,0,0.04)" }}>
                  <div style={{ display:"flex", justifyContent:"space-between", marginBottom:4 }}>
                    <div style={{ fontWeight:"bold", color:"#2d3748" }}>{entry.name}</div>
                    <div style={{ fontSize:11, color:"#4a7c59" }}>✓ Sent</div>
                  </div>
                  <div style={{ fontSize:12, color:"#8a6f4e", marginBottom:6, fontFamily:"monospace" }}>{formatPhone(entry.phone)}</div>
                  {(entry.msgs || [entry.msg]).map((m, mi) => (
                    <div key={mi} style={{ fontSize:13, color:"#555", fontStyle:"italic", marginBottom:3 }}>
                      {(entry.msgs?.length > 1) && <span style={{ fontSize:10, color:"#aaa", fontStyle:"normal" }}>Text {mi+1}: </span>}
                      "{m}"
                    </div>
                  ))}
                  <div style={{ fontSize:11, color:"#bbb", marginTop:6 }}>{entry.time}</div>
                </div>
              ))
            }
          </div>
        )}
      </div>
      <style>{`@keyframes fadeIn{from{opacity:0;transform:translateX(-50%) translateY(-8px);}to{opacity:1;transform:translateX(-50%) translateY(0);}}`}</style>
    </div>
  );
}
