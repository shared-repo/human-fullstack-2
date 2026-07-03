import ContactCard from "./ContactCard";

function ContactCardList(props) {

    const { contacts } = props; // destructuring : props = { contacts : [ {}, {}, {}] }

    return (
        <>
            {/* 
            <ContactCard contact={contacts[0]} />
            <ContactCard contact={contacts[1]} />
            <ContactCard contact={contacts[2]} /> 
            */}

            {
                contacts.map((c, idx) => {
                    return <ContactCard key={idx} contact={c} />
                })
            }

        </>
    )

}

export default ContactCardList;